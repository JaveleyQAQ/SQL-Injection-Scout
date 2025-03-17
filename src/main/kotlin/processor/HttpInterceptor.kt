package processor

import ExecutorManager
import model.logentry.ModifiedLogEntry
import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.ToolType
import burp.api.montoya.http.handler.*
import burp.api.montoya.http.message.ContentType
import burp.api.montoya.http.message.HttpRequestResponse
import burp.api.montoya.http.message.params.HttpParameterType
import burp.api.montoya.http.message.params.ParsedHttpParameter
import burp.api.montoya.http.message.requests.HttpRequest
import com.github.difflib.DiffUtils
import com.nickcoblentz.montoya.PayloadUpdateMode
import com.nickcoblentz.montoya.withUpdatedParsedParameterValue
import config.Configs
import model.logentry.LogEntry

import utils.RequestResponseUtils
import java.awt.Color
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import javax.swing.SwingUtilities
import com.nickcoblentz.montoya.sendRequestWithUpdatedContentLength
import com.nickcoblentz.montoya.withUpdatedContentLength
import model.logentry.ModifiedLogDataModel
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class HttpInterceptor(
    private val logs: LogEntry,
    private val api: MontoyaApi,
    private val modifiedLog: ModifiedLogEntry,
) : HttpHandler {


    val executorService = ExecutorManager.get().executorService
    private val configs = Configs.INSTANCE
    private val requestResponseUtils = RequestResponseUtils()
    private var uniqScannedParameters: MutableSet<String> = HashSet() // 记录已扫描的参数
    private var requestPayloadMap: MutableMap<HttpRequest?, Pair<String, String>> = HashMap() // 记录请求的参数和payload
    private val output = api.logging()
    private val requestTimeout = 60000L // 10秒超时


    override fun handleHttpRequestToBeSent(p0: HttpRequestToBeSent?): RequestToBeSentAction {
        return RequestToBeSentAction.continueWith(p0)
    }


    override fun handleHttpResponseReceived(responseReceived: HttpResponseReceived): ResponseReceivedAction {

        executorService.submit {
            if (responseReceived.toolSource().isFromTool(ToolType.SCANNER) || responseReceived.toolSource()
                    .isFromTool(ToolType.EXTENSIONS) || responseReceived.toolSource().isFromTool(ToolType.INTRUDER)
            ) return@submit

            if (responseReceived != null) processHttpHandler(responseReceived)
        }
        return ResponseReceivedAction.continueWith(responseReceived)
    }


    fun processHttpHandler(responseReceived: HttpResponseReceived) {


        if (!requestResponseUtils.isRequestAllowed(responseReceived)) return

        val originalRequest = responseReceived.initiatingRequest()
        val tmpParametersMD5 =
            requestResponseUtils.calculateMD5(originalRequest.parameters().filter { it.type().name != "COOKIE" }
                .map { "${originalRequest.url().split('?')[0]} | ${it.name()} | ${it.type()}" }.toString())

        if (requestResponseUtils.getAllowedParamsCounts(originalRequest) > configs.maxAllowedParameterCount) {
            val originalRequestResponse = HttpRequestResponse.httpRequestResponse(originalRequest, responseReceived)
            logs.markRequestWithExcessiveParameters(tmpParametersMD5, originalRequestResponse)
            output.logToError("${originalRequestResponse.request().path()} 请求参数超出允许最大参数数量！")
            return
        }
        val originalRequestResponse = HttpRequestResponse.httpRequestResponse(originalRequest, responseReceived)


        val logIndex = logs.add(tmpParametersMD5, originalRequestResponse)
        if (logIndex >= 0) {
            val parameters = originalRequest.parameters()
            val newRequests =  GenerateRequests.processRequests(originalRequest)
            requestPayloadMap = GenerateRequests.getRequestPayloadMap()
//                generateRequestByPayload(originalRequest, parameters, configs.payloads)
            output.logToOutput(
                "[+] Scanning: ${
                    originalRequest.url().split('?')[0]
                }｜ ParamsCount： ${parameters.count { it.type().name != "COOKIE" }} | ReqCount：${newRequests.size}"
            )

            modifiedLog.addExpectedEntriesForMD5(tmpParametersMD5, newRequests.size)
            scheduleRequests(newRequests, tmpParametersMD5)
        }

    }

    fun scheduleRequests(newRequests: List<HttpRequest>, tmpParametersMD5: String) {
        newRequests.chunked(5).forEachIndexed { batchIndex, batch ->
            batch.forEach { newRequest ->
                val batchDelay = batchIndex * configs.fixedIntervalTime
                val randomDelay = Random.nextLong(0, configs.randomCheckTimer / 2)
                val delay = batchDelay + randomDelay

                // 提交任务到线程池
                val future = executorService.schedule({
                    // 记录任务实际开始时间
                    val startTime = System.currentTimeMillis()

                    try {
                        val response = api.http().sendRequestWithUpdatedContentLength(newRequest)
                        response?.let { processResponse(tmpParametersMD5, it) }
                    } catch (e: Exception) {
                        api.logging().logToError("Error sending request", e)
                    } finally {
                        // 计算任务实际耗时
                        val elapsedTime = System.currentTimeMillis() - startTime
                        if (elapsedTime > requestTimeout) {
                            api.logging().logToError("Request ${newRequest.url()} took too long: ${elapsedTime}ms")
                        }
                    }
                }, delay, TimeUnit.MILLISECONDS)

                // 超时控制（从任务实际开始执行时计时）
                executorService.schedule({
                    if (!future.isDone) {
                        future.cancel(true)
                        api.logging().logToError("Request timeout for: ${newRequest.url()}")
                        // 强制处理超时请求
                        processResponse(tmpParametersMD5, HttpRequestResponse.httpRequestResponse(newRequest,null))
                    }
                }, requestTimeout + delay, TimeUnit.MILLISECONDS) // 超时时间包含初始延迟
            }
        }
    }

    private fun readAllLinesFromByteArray(byteArray: ByteArray?, charset: Charset): List<String> {
        if (byteArray == null) return emptyList()
        val inputStream = ByteArrayInputStream(byteArray)
        val reader = BufferedReader(InputStreamReader(inputStream, charset))
        val result = mutableListOf<String>()
        reader.use {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                result.add(line!!)
            }
        }

        return result
    }

    /**
     * 对修改后的响应做处理
     */
    private fun processResponse(md5: String, httpRequestResponse: HttpRequestResponse) {

        val response = httpRequestResponse.response()
        val request = httpRequestResponse.request()
        val (parameter, payload) = requestPayloadMap[request] ?: return
        val originalRequestResponse = logs.getEntry(md5)?.requestResponse
        if (originalRequestResponse != null) {
                val checkSQL = requestResponseUtils.checkErrorSQLException(response.bodyToString())    //检测response是否存在sqlError
//                val isBoring = requestResponseUtils.isBoringWordInResponse(response) // Boring 单词匹配
                val checkBoring =  requestResponseUtils.checkBoringWordInResponse(response)

                if (!checkBoring.isNullOrEmpty()) {
                    modifiedLog.addModifiedEntry(md5, ModifiedLogDataModel(md5,parameter,payload,
                        "match boring",httpRequestResponse.response().statusCode(),false,httpRequestResponse,"0"),
                        checkBoring)
                } else {
                    // 使用处理响应, 并计算model信息
                    var (modifiedEntry, diffText) = requestResponseUtils.processResponseWithDifference(
                        logs, md5, httpRequestResponse, parameter, payload, checkSQL
                    )
                    // 对比差异
                    val originalBody =
                        readAllLinesFromByteArray(originalRequestResponse.response().body().bytes, Charsets.UTF_8)
                    val revisedBody = readAllLinesFromByteArray(response?.body()?.bytes, Charsets.UTF_8)
                    val diffs = DiffUtils.diff(originalBody, revisedBody).deltas
                    // 取出第一个不等的地方，作为diffText
                    diffText = when {
                        diffs.isEmpty() -> ""
                        diffs.size >= 1 -> diffs[0].target.lines.getOrElse(0) { "" } //不管几个地方不同，都只取第一个
                        else -> ""
                    }

                    // null/Null存在差异，但 diff 长度却相同  {"count": 1, "data":{}} diff {"count": 0, "data":{}}
                    if (payload.equals(null, ignoreCase = true) && modifiedEntry.diff == "same" && diffText != "") {
                        modifiedEntry.diff = "sameLen diff detail"
                        modifiedEntry.color = listOf(Color.YELLOW, null)
                        diffText = "The responses have the same length but different contents.  $diffText"
                    }

                    // 若存在 SQL 匹配项，则标记为红色并记录
                    if (!checkSQL.isNullOrEmpty()) {
                        api.logging()
                            .logToOutput("[+] ${request.url()}] parameter [$parameter] using  payload [$payload] match  response [$checkSQL] ✅")
                        logs.setVulnerability(md5, true)
                        modifiedEntry.color = listOf(Color.RED, null)
                        diffText = checkSQL
                    }
                    // 解决 部分参数设置为 null时， 302/401状态存在差异时候 有趣的/绿色的
                    if (payload == "null" && modifiedEntry.status.toString() != "200") {
                        modifiedEntry.color = listOf(Color.LIGHT_GRAY, null)
                    }
                    modifiedLog.addModifiedEntry(md5, modifiedEntry, diffText)
                }

        }
    }
}
