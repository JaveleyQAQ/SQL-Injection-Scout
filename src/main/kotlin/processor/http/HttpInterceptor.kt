package processor.http

import burp.api.montoya.MontoyaApi
import burp.api.montoya.http.handler.HttpHandler
import burp.api.montoya.http.handler.HttpRequestToBeSent
import burp.api.montoya.http.handler.HttpResponseReceived
import burp.api.montoya.http.handler.RequestToBeSentAction
import burp.api.montoya.http.handler.ResponseReceivedAction
import burp.api.montoya.http.message.HttpRequestResponse
import burp.api.montoya.http.message.requests.HttpRequest
import com.github.difflib.DiffUtils
import com.nickcoblentz.montoya.sendRequestWithUpdatedContentLength
import config.Configs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import model.logentry.LogEntry
import model.logentry.ModifiedLogDataModel
import model.logentry.ModifiedLogEntry
import processor.helper.payload.GenerateRequests
import utils.RequestResponseUtils
import java.awt.Color
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class HttpInterceptor(
    private val logs: LogEntry,
    private val api: MontoyaApi,
    private val modifiedLog: ModifiedLogEntry,
) : HttpHandler {


    private val executorService = ExecutorManager.get().executorService
    private val configs = Configs.INSTANCE
    private val requestResponseUtils = RequestResponseUtils()
    private var requestPayloadMap: MutableMap<HttpRequest?, Pair<String, String>> = HashMap() // 记录请求的参数和payload
    private val output = api.logging()
    private val requestTimeout = 60000L // 10秒超时

    private inline fun <T> safeExecute(block: () -> T): T? = try {
        block()
    } catch (e: Exception) {
        output.logToError("Unexpected error in ${Thread.currentThread().name}: ${e.message}", e)
        null
    }

    override fun handleHttpRequestToBeSent(p0: HttpRequestToBeSent?): RequestToBeSentAction {
        return RequestToBeSentAction.continueWith(p0)
    }


    override fun handleHttpResponseReceived(response: HttpResponseReceived): ResponseReceivedAction {
        executorService.submit {
            if (!requestResponseUtils.checkConfigsChoseBox(response)) return@submit
            val originalRequestResponse = HttpRequestResponse.httpRequestResponse(
                response.initiatingRequest(), response
            )
            safeExecute { processHttpHandler(originalRequestResponse) }
        }
        return ResponseReceivedAction.continueWith(response)
    }

    fun processHttpHandler(httpRequestResponse: HttpRequestResponse) {
        val originalRequest = httpRequestResponse.request()
        val tmpParametersMD5 = requestResponseUtils.calculateParameterHash(originalRequest)

        if (!requestResponseUtils.isRequestAllowed(logs, output, tmpParametersMD5, httpRequestResponse)) return

        val logIndex = logs.add(tmpParametersMD5, httpRequestResponse)
        if (logIndex >= 0) {
            val parameters = originalRequest.parameters().filterNot { it.type().name == "COOKIE" }
            val newRequests = GenerateRequests.processRequests(originalRequest)
            requestPayloadMap = GenerateRequests.getRequestPayloadMap()
            output.logToOutput(
                "[+] Scanning: ${originalRequest.url().split('?').first()} " +
                        "| Params: ${parameters.size} | Requests: ${newRequests.size}"
            )

            modifiedLog.addExpectedEntriesForMD5(tmpParametersMD5, newRequests.size)
            scheduleRequests(newRequests, tmpParametersMD5)
        }

    }

    private fun scheduleRequests(newRequests: List<HttpRequest>, parameterHash: String) {
        val batchSize = (configs.fixedIntervalTime / 50).coerceIn(3, 10).toInt()
        newRequests.chunked(batchSize).forEachIndexed { batchIndex, batch ->
            batch.forEach { newRequest ->
                // 计算动态延迟（指数退避 + 抖动）
                val baseDelay = batchIndex * configs.fixedIntervalTime
                val jitter = Random.nextLong(configs.randomCheckTimer / 4, configs.randomCheckTimer / 2)
                val delay = baseDelay + jitter

                // 使用主调度器提交任务（复用线程池）
                executorService.schedule({
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            withTimeout(requestTimeout) {
                                val startTime = System.nanoTime()
                                val response = api.http().sendRequestWithUpdatedContentLength(newRequest)
                                processResponse(parameterHash, response)

                            }
                        } catch (e: TimeoutCancellationException) {
                            output.logToError("[-] Request timeout: ${newRequest.url()}")
                            processResponse(parameterHash, HttpRequestResponse.httpRequestResponse(newRequest, null))
                        } catch (e: Exception) {
                            output.logToError("[-] Request failed: ${newRequest.url()}")
                        }
                    }
                }, delay, TimeUnit.MILLISECONDS)
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

        val request = httpRequestResponse.request() ?: return
        val response = httpRequestResponse.response() ?: return
        val (parameter, payload) = requestPayloadMap[request] ?: return
        val originalRequestResponse = logs.getEntry(md5)?.requestResponse ?: return

        // 检查 SQL 错误和 Boring 关键字
        val checkSQL = requestResponseUtils.checkErrorSQLException(response.bodyToString())
        val checkBoring = requestResponseUtils.checkBoringWordInResponse(response)

        when {
            !checkBoring.isNullOrEmpty() -> {
                modifiedLog.addModifiedEntry(
                    md5, ModifiedLogDataModel(
                        md5, parameter, payload,
                        "match boring", httpRequestResponse.response().statusCode(), false, httpRequestResponse, "0"
                    ),
                    checkBoring
                )
            }
            else -> {
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
                    diffs.isNotEmpty() -> diffs[0].target.lines.getOrElse(0) { "" } //不管几个地方不同，都只取第一个
                    else -> ""
                }

                // null/Null存在差异，但 diff 长度却相同  {"count": 1, "data":{}} diff {"count": 0, "data":{}}
                if (payload.equals(
                        "null",
                        ignoreCase = true
                    ) && modifiedEntry.diff == "same" && diffText.isNullOrEmpty()
                ) {
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