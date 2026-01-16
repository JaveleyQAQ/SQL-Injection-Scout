package processor.http

import burp.api.montoya.MontoyaApi
import burp.api.montoya.http.handler.*
import burp.api.montoya.http.message.HttpRequestResponse
import burp.api.montoya.http.message.requests.HttpRequest
import com.github.difflib.DiffUtils
import com.nickcoblentz.montoya.sendRequestWithUpdatedContentLength
import config.Configs
import config.ExecutorManager
import kotlinx.coroutines.*
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
import java.util.concurrent.ConcurrentHashMap
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

    // 1. 优化：使用 ConcurrentHashMap 保证线程安全
    private val requestPayloadMap: MutableMap<HttpRequest, Pair<String, String>> = ConcurrentHashMap()

    private val output = api.logging()
    private val requestTimeout = 60000L // 60秒超时

    // 4. 优化：类级别的协程作用域，避免反复创建
    // SupervisorJob 确保一个子任务失败不会导致整个 Scope 取消
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
        // 使用 Burp 的线程池或自定义线程池处理耗时任务，避免阻塞 HTTP 处理流
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

        // 添加到主日志
        val logIndex = logs.add(tmpParametersMD5, httpRequestResponse)

        if (logIndex >= 0) {
            val parameters = originalRequest.parameters().filterNot { it.type().name == "COOKIE" }

            // 生成 Fuzz 请求
            val newRequests = GenerateRequests.processRequests(originalRequest, tmpParametersMD5)

            // 使用 putAll 而不是覆盖，防止并发扫描时丢失其他任务的数据
            requestPayloadMap.putAll(GenerateRequests.getRequestPayloadMap() as Map<out HttpRequest, Pair<String, String>>)
            output.logToOutput(
                "[+] Scanning: ${originalRequest.url().split('?').first()} " +
                        "| Params: ${parameters.size} | Requests: ${newRequests.size}"
            )

            modifiedLog.addExpectedEntriesForMD5(tmpParametersMD5, newRequests.size)
            scheduleRequests(newRequests, tmpParametersMD5)
        }
    }

    private fun scheduleRequests(newRequests: List<HttpRequest>, parameterHash: String) {
        // 动态批次大小：每 50ms 发送一个请求作为基准，限制在 3-10 之间
        val batchSize = (configs.fixedIntervalTime / 50).coerceIn(3, 10).toInt()

        newRequests.chunked(batchSize).forEachIndexed { batchIndex, batch ->
            scope.launch {
                val baseDelay = batchIndex * configs.fixedIntervalTime
                val jitter = Random.nextLong(configs.randomCheckTimer / 4, configs.randomCheckTimer / 2)
                val totalDelay = baseDelay + jitter

                // 挂起直到延迟结束，不阻塞线程
                delay(totalDelay)

                // 并发发送当前批次
                batch.forEach { newRequest ->
                    launch {
                        try {
                            withTimeout(requestTimeout) {
                                // 发送请求
                                val response = api.http().sendRequestWithUpdatedContentLength(newRequest)
                                processResponse(parameterHash, response)
                            }
                        } catch (e: TimeoutCancellationException) {
                            output.logToError("[-] Request timeout: ${newRequest.url()}")
                            // 超时也记录，方便排查
                            processResponse(parameterHash, HttpRequestResponse.httpRequestResponse(newRequest, null))
                        } catch (e: Exception) {
                            output.logToError("[-] Request failed: ${newRequest.url()} - ${e.message}")
                        }
                    }
                }
            }
        }
    }
    /**
     * 对修改后的响应做处理
     */
    private fun processResponse(md5: String, httpRequestResponse: HttpRequestResponse) {
        val request = httpRequestResponse.request() ?: return
        val (parameter, payload) = requestPayloadMap.remove(request) ?: return
        val response = httpRequestResponse.response() ?: return
        val originalRequestResponse = logs.getEntry(md5)?.requestResponse ?: return

        val responseBodyStr = response.bodyToString()

        //检查 SQL 错误 (直接使用 String 匹配)
        val checkSQL = requestResponseUtils.checkErrorSQLException(responseBodyStr)
        val checkBoring = requestResponseUtils.checkBoringWordInResponse(response)

        when {
            !checkBoring.isNullOrEmpty() -> {
                modifiedLog.addModifiedEntry(
                    md5, ModifiedLogDataModel(
                        md5, parameter, payload,
                        "match boring", response.statusCode(), false, httpRequestResponse, "0"
                    ),
                    checkBoring
                )
            }
            else -> {
                // 使用处理响应, 并计算model信息
                var (modifiedEntry, diffText) = requestResponseUtils.processResponseWithDifference(
                    logs, md5, httpRequestResponse, parameter, payload, checkSQL
                )

                val originalBodyList = originalRequestResponse.response().bodyToString().lines().take(1000)
                val revisedBodyList = responseBodyStr.lines().take(1000)

                // 只有当内容前1000行不同时才计算 Diff
                if (originalBodyList != revisedBodyList) {
                    try {
                        val diffs = DiffUtils.diff(originalBodyList, revisedBodyList).deltas
                        diffText = when {
                            diffs.isNotEmpty() -> {
                                val line = diffs[0].target.lines.getOrElse(0) { "" }
                                line.trim()
                            }
                            else -> ""
                        }
                    } catch (e: Exception) {
                        diffText = "Diff Error"
                    }
                }

                // 若存在 SQL 匹配项，则标记为红色并记录
                if (!checkSQL.isNullOrEmpty()) {
                    output.logToOutput("[+] ${request.url()}] parameter [$parameter] using payload [$payload] match response [$checkSQL] ✅")
                    logs.setVulnerability(md5, true)
                    modifiedEntry.color = listOf(Color.RED, null)
                    diffText = checkSQL
                }

                // 部分参数设置为 null时， 302/401状态存在差异时候 有趣的/绿色的
                if (payload == "null" && modifiedEntry.status.toString() != "200") {
                    modifiedEntry.color = listOf(Color.LIGHT_GRAY, null)
                }
                modifiedLog.addModifiedEntry(md5, modifiedEntry, diffText)
            }
        }
    }
}