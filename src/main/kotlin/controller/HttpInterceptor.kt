package controller

import ExecutorManager
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
import model.logentry.ModifiedLogEntry
import utils.RequestResponseUtils
import java.awt.Color
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
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
    private val requestTimeout = 10000L // 10秒超时

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
            logs.markRequestWithExcessiveParameters(tmpParametersMD5,originalRequestResponse)
            output.logToError("${originalRequestResponse.request().path()} 请求参数超出允许最大参数数量！")
            return
        }
        val originalRequestResponse = HttpRequestResponse.httpRequestResponse(originalRequest, responseReceived)


        val logIndex = logs.add(tmpParametersMD5, originalRequestResponse)
        if (logIndex >= 0) {
            val parameters = originalRequest.parameters()
            val newRequests = generateRequestByPayload(originalRequest, parameters, configs.payloads)
            output.logToOutput(
                "｜开始扫描: ${
                    originalRequest.url().split('?')[0]
                }｜ 总参数数量： ${parameters.count { it.type().name != "COOKIE" }} | 预计请求数：${newRequests.size}"
            )
            modifiedLog.addExpectedEntriesForMD5(tmpParametersMD5, newRequests.size)
            scheduleRequests(newRequests, tmpParametersMD5)
        }

    }

    fun scheduleRequests(newRequests: List<HttpRequest>, tmpParametersMD5: String) {
        val batchSize = 5
        newRequests.chunked(batchSize).forEachIndexed { batchIndex, batch ->
            batch.forEach { newRequest ->
                val batchDelay = batchIndex * configs.fixedIntervalTime
                val randomDelay = Random.nextLong(0, configs.randomCheckTimer / 2)
                val delay = batchDelay + randomDelay

                val future = executorService.schedule({
                    try {
                        val response = api.http().sendRequestWithUpdatedContentLength(newRequest)
                        if (response != null) {
                            processResponse(tmpParametersMD5, response)
                        }
                    } catch (e: Exception) {
                        api.logging().logToError("Error sending request", e)
                    }
                }, delay, TimeUnit.MILLISECONDS)

                // 添加超时控制
                executorService.schedule({
                    if (!future.isDone) {
                        future.cancel(true)
                        api.logging().logToError("Request timeout for: ${newRequest.url()}")
                    }
                }, requestTimeout, TimeUnit.MILLISECONDS)
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
        SwingUtilities.invokeLater {
            //检测response是否存在sqlError
            val checkSQL = requestResponseUtils.checkErrorSQLException(response.body().toString())
            val originalRequestResponse = logs.getEntry(md5)?.requestResponse
            // 使用新方法处理响应
            var (modifiedEntry, diffText) = requestResponseUtils.processResponseWithDifference(
                logs, md5, httpRequestResponse, parameter, payload, checkSQL
            )
            // 对比差异
            val originalBody =
                readAllLinesFromByteArray(originalRequestResponse?.response()?.body()?.bytes, Charsets.UTF_8)
            val revisedBody = readAllLinesFromByteArray(response?.body()?.bytes, Charsets.UTF_8)
            val diffs = DiffUtils.diff(originalBody, revisedBody).deltas
            // 取出第一个不等的地方，作为diffText
            diffText = when {
                diffs.isEmpty() -> ""
                diffs.size >= 1 -> diffs[0].target.lines.getOrElse(0) { "" } //不管几个地方不同，都只取第一个
                else -> ""
            }

            // 存在差异，但 diff 长度却相同  {"count": 1, "data":{}} diff {"count": 0, "data":{}}
            if (modifiedEntry.diff == "same" && diffText != "") {
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
            if (payload == "null" && modifiedEntry.status.toString()!="200") {
                modifiedEntry.color = listOf(Color.LIGHT_GRAY, null)
            }


            modifiedLog.addModifiedEntry(md5, modifiedEntry, diffText)
        }
    }

    /**
     * 生成恶意请求
     */
    private fun generateRequestByPayload(
        request: HttpRequest,
        parameters: List<ParsedHttpParameter>,
        payloads: List<String>,
    ): List<HttpRequest> {

        val requestListWithPayload = mutableListOf<HttpRequest>()
        addEmptyJsonRequest(request, requestListWithPayload)
        addAllParamsNullRequest(request, parameters, requestListWithPayload)

        //参数处理
        parameters.forEach { parameter ->
            if (!isParameterShippable(parameter)) {
                val parameterFlag = createParameterFlag(request, parameter)
                if (!uniqScannedParameters.contains(parameterFlag)) {
                    uniqScannedParameters.add(parameterFlag)
                    var currentPayloads = listOf<String>()
                    currentPayloads = preparePayloads(parameter.value(), payloads)
                    currentPayloads.forEach { payload ->
                        val mode = PayloadUpdateMode.APPEND
                        if (payload == "null") PayloadUpdateMode.REPLACE else PayloadUpdateMode.APPEND
                        val newRequest = addPayloadToRequestParam(request, parameter, payload, mode)
                        requestListWithPayload.add(newRequest)
                        requestPayloadMap[newRequest] = Pair(parameter.name(), payload)

                    }
                    // 插入null到单个参数,如果就一个参数 单独设置会和addAllParamsNullRequest重复
                    if (configs.nullCheck && parameters.size >= 2) {
                        val req = requestResponseUtils.replaceJsonParameterValueWithNull(request, parameter)
                        requestListWithPayload.add(req)
                        requestPayloadMap[req] = Pair(parameter.name(), "null")
                    }

                }
            }
        }

        return requestListWithPayload
    }

    /**
     * 检测参数位置是否值得跳过
     */
    private fun isParameterShippable(parameter: ParsedHttpParameter): Boolean {
        return parameter.type().name.uppercase() == "COOKIE"
    }

    /**
     * 创建参数flag，用于扫描去重筛选
     */
    private fun createParameterFlag(request: HttpRequest, parameter: ParsedHttpParameter): String {
        return "${request.path()}||${parameter.name()}||${parameter.type().name}"
    }

    /**
     * 考虑是否需要 二次处理payload
     * 这里对int 进行-1处理
     */
    private fun preparePayloads(parameterValue: String, originalPayloads: List<String>): List<String> {
        // 备份原始 payloads 的副本，不然每次add都会修改原始payloads
        val mutablePayloads = originalPayloads.toMutableList()
        if (requestResponseUtils.parameterValueIsInteger(parameterValue)) {
            mutablePayloads.add("-1")
        }
        return mutablePayloads
    }

    /**
     * 将json请求设置为空json
     */
    private fun addEmptyJsonRequest(request: HttpRequest, requestListWithPayload: MutableList<HttpRequest>) {
        if (configs.nullCheck && request.contentType() == ContentType.JSON) {
            val emptyRequest = HttpRequest.httpRequest(
                request.httpService(),
                "${request.toString().substring(0, request.bodyOffset())}{}"
            ).withUpdatedContentLength()
            requestListWithPayload.add(emptyRequest)
            requestPayloadMap[emptyRequest] = Pair("{}", "{}")
        }
    }

    /**
     * 将所有参数值设置为null
     */
    private fun addAllParamsNullRequest(
        request: HttpRequest,
        parameters: List<ParsedHttpParameter>,
        requestListWithPayload: MutableList<HttpRequest>
    ) {
        // 所有参数值设置为 null
        if (configs.nullCheck) {
            val allValuesWithNull =
                requestResponseUtils.replaceAllParameterValuesWithNull(request, parameters)
                    .withUpdatedContentLength(true)
            requestListWithPayload.add(allValuesWithNull)
            requestPayloadMap[allValuesWithNull] = Pair("ALL param", "NULL")
        }

    }

    /**
     * 将payload插入参数
     */
    private fun addPayloadToRequestParam(
        request: HttpRequest,
        parameter: ParsedHttpParameter,
        payload: String,
        module: PayloadUpdateMode
    ): HttpRequest {
        return when (parameter.type()) {
            HttpParameterType.JSON -> {
                val valueType = requestResponseUtils.jsonValueType(request, parameter)
                if (valueType != Int && !(payload.contains("'") || payload.contains("\""))) {
                    return request // Skip
                }
                return request.withUpdatedParsedParameterValue(
                    parameter,
                    payload.replace("\"", "%22", true).replace("#", "%23", true),
                    module
                ).withUpdatedContentLength(true)
            }

            else ->
                return request.withUpdatedParsedParameterValue(
                    parameter,
                    api.utilities().urlUtils().encode(payload),
                    module
                ).withUpdatedContentLength(true)


        }


    }
}
