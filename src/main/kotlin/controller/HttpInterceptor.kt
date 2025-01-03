package controller

import ExecutorManager
import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.ToolSource
import burp.api.montoya.core.ToolType
import burp.api.montoya.http.RedirectionMode
import burp.api.montoya.http.RequestOptions
import burp.api.montoya.http.handler.*
import burp.api.montoya.http.message.ContentType
import burp.api.montoya.http.message.HttpRequestResponse
import burp.api.montoya.http.message.params.ParsedHttpParameter
import burp.api.montoya.http.message.requests.HttpRequest
import com.github.difflib.DiffUtils
import com.nickcoblentz.montoya.PayloadUpdateMode
import com.nickcoblentz.montoya.withUpdatedParsedParameterValue
import config.Configs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import model.logentry.LogEntry
import model.logentry.ModifiedLogEntry
import utils.RequestResponseUtils
import java.awt.Color
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.concurrent.CompletableFuture
import javax.swing.SwingUtilities
import com.nickcoblentz.montoya.sendRequestWithUpdatedContentLength
import com.nickcoblentz.montoya.withUpdatedContentLength
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
    private val uniqScannedParameters: MutableSet<String> = HashSet() // 记录已扫描的参数
    private val requestPayloadMap: MutableMap<HttpRequest?, Pair<String, String>> = HashMap() // 记录请求的参数和payload
    private val output = api.logging()

    fun clear() {
        uniqScannedParameters.clear()
        requestPayloadMap.clear()
    }

    override fun handleHttpRequestToBeSent(p0: HttpRequestToBeSent?): RequestToBeSentAction {
        return RequestToBeSentAction.continueWith(p0)
    }


    override fun handleHttpResponseReceived(responseReceived: HttpResponseReceived): ResponseReceivedAction {

        executorService.submit {
            if (responseReceived.toolSource().isFromTool(ToolType.SCANNER) || responseReceived.toolSource()
                    .isFromTool(ToolType.EXTENSIONS) || responseReceived.toolSource().isFromTool(ToolType.INTRUDER)
            ) return@submit

            if (responseReceived != null) processHttpResponse(responseReceived)
        }
        return ResponseReceivedAction.continueWith(responseReceived)
    }


    private fun processHttpResponse(responseReceived: HttpResponseReceived) {


        if (!requestResponseUtils.isRequestAllowed(responseReceived)) return

        val originalRequest = responseReceived.initiatingRequest()

        val originalRequestResponse = HttpRequestResponse.httpRequestResponse(originalRequest, responseReceived)

        val tmpParametersMD5 =
            requestResponseUtils.calculateMD5(originalRequest.parameters().filter { it.type().name != "COOKIE" }
                .map { "${originalRequest.url().split('?')[0]} | ${it.name()} | ${it.type()}" }.toString())
        val logIndex = logs.add(tmpParametersMD5, originalRequestResponse)
        if (logIndex >= 0) {
            val parameters = originalRequest.parameters()
            val newRequests = generateRequestByPayload(originalRequest, parameters, configs.payloads)
            output.logToOutput(
                "｜开始扫描: ${
                    originalRequest.url().split('?')[0]
                }｜ 总参数数量： ${parameters.size} | 预计请求数：${newRequests.size}"
            )
            modifiedLog.addExpectedEntriesForMD5(tmpParametersMD5, newRequests.size)
            scheduleRequests(newRequests,tmpParametersMD5)
        }
    }


    fun scheduleRequests(newRequests: List<HttpRequest> ,tmpParametersMD5: String) {
        newRequests.forEachIndexed { index, newRequest ->
            // 使用固定的增量来确保每个请求之间有足够的间隔
            val delay = (index * configs.fixedIntervalTime) + Random.nextLong(0, configs.randomCheckTimer) // 每个请求至少间隔1秒，并且有额外的随机抖动
            executorService.schedule({
                try {
                    val response = api.http().sendRequestWithUpdatedContentLength(newRequest)
                    if (response != null) {
                        processResponse(tmpParametersMD5, response)
                    }
                } catch (e: Exception) {
                    api.logging().logToError("Error sending request", e)
                }
            }, delay, TimeUnit.MILLISECONDS)
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


    private fun processResponse(md5: String, httpRequestResponse: HttpRequestResponse) {

        val response = httpRequestResponse.response()
        val request = httpRequestResponse.request()
        val checkSQL = requestResponseUtils.checkErrorSQLException(response.body().toString())



        val (parameter, payload) = requestPayloadMap[request] ?: return
        SwingUtilities.invokeLater {
            val originalRequestResponse = logs.getEntry(md5)?.requestResponse
            // 使用新方法处理响应
            var (modifiedEntry, diffText) = requestResponseUtils.processResponseWithDifference(
                logs, md5, httpRequestResponse, parameter, payload, checkSQL
            )


            val originalBody =
                readAllLinesFromByteArray(originalRequestResponse?.response()?.body()?.bytes, Charsets.UTF_8)
            val revisedBody = readAllLinesFromByteArray(response?.body()?.bytes, Charsets.UTF_8)
            val diffs = DiffUtils.diff(originalBody, revisedBody).deltas


            diffText = when {
                diffs.isEmpty() -> ""
                diffs.size >= 1 -> diffs[0].target.lines.getOrElse(0) { "" } //不管几个地方不同，都只取第一个
                else -> ""
            }
            if (!checkSQL.isNullOrEmpty()) {
                api.logging()
                    .logToOutput("[${request.url()}] parameter [$parameter] using  payload [$payload] match  response [$checkSQL] ✅")
                logs.setVulnerability(md5, true)
                modifiedEntry.color = listOf(Color.RED, null)
                diffText = checkSQL
            }
            modifiedLog.addModifiedEntry(md5, modifiedEntry, diffText)
        }
    }

    private fun generateRequestByPayload(
        request: HttpRequest,
        parameters: List<ParsedHttpParameter>,
        payloads: MutableList<String>,
    ): List<HttpRequest> {
        val payloadRequestList = mutableListOf<HttpRequest>()

        for (param in parameters) {
            // 记录 path-paramName-ParamType格式避免重复扫描
            val parameterKey = "${request.path()}||${param.name()}||${param.type().name}"
            if (param.type().name.uppercase() !== "COOKIE" && !requestResponseUtils.parameterValueIsBoolean(param.value())) {
                if (!uniqScannedParameters.contains(parameterKey)) {
                    // 创建一个新的列表来存储当前参数的 payload
                    val currentPayloads = mutableListOf<String>()

                    when {
                        requestResponseUtils.parameterValueIsInteger(param.value()) -> {
                            // 对于整数类型的参数，除了原始的 payloads，还添加 -1
                            currentPayloads.addAll(payloads)
                            currentPayloads.add("-1")
                        }

                        else -> {
                            // 对于非整数类型的参数，只使用原始的 payloads
                            currentPayloads.addAll(payloads)
                        }
                    }

                    for (payload in currentPayloads) {
                        var requestWithPayload: HttpRequest? = null
                        if (request.contentType() != ContentType.JSON) {
                            requestWithPayload =
                                request.withUpdatedParsedParameterValue(
                                    param,
                                    api.utilities().urlUtils().encode(payload),
                                    PayloadUpdateMode.APPEND
                                )

                            requestWithPayload = requestWithPayload.withUpdatedContentLength(true)
                            payloadRequestList.add(requestWithPayload)
                        } else if (request.contentType() == ContentType.JSON) {
                            requestWithPayload = request.withUpdatedParsedParameterValue(
                                param,
                                payload.replace("\"", "%22", true).replace("#", "%23", true),
                                PayloadUpdateMode.APPEND
                            )
                            requestWithPayload = requestWithPayload.withUpdatedContentLength(true)
                            payloadRequestList.add(requestWithPayload)
                        }

                        requestPayloadMap[requestWithPayload] = Pair(param.name(), payload)
                    }
                    uniqScannedParameters.add(parameterKey)
                }
            }
        }

        return payloadRequestList
    }
}
