package utils

import burp.api.montoya.core.Marker
import burp.api.montoya.core.ToolType
import burp.api.montoya.http.handler.HttpResponseReceived
import burp.api.montoya.http.message.HttpRequestResponse
import burp.api.montoya.http.message.params.HttpParameter
import burp.api.montoya.http.message.params.HttpParameterType
import burp.api.montoya.http.message.params.ParsedHttpParameter
import burp.api.montoya.http.message.requests.HttpRequest
import burp.api.montoya.http.message.responses.HttpResponse
import com.nickcoblentz.montoya.withUpdatedContentLength
import config.Configs
import model.logentry.LogEntry
import model.logentry.ModifiedLogDataModel
import java.math.BigInteger
import java.security.MessageDigest
import java.util.regex.Pattern

class RequestResponseUtils {
    private val configs = Configs.INSTANCE

    fun getResponseTile(body: ByteArray?): String {
        if (body?.isEmpty() == true) {
            return " "
        }
        val pattern = Pattern.compile(
            "<\\s*title.*?>([^<]+)<\\s*/\\s*title>",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        );
        val bodyString = java.lang.String(body, Charsets.UTF_8)
        val matcher = pattern.matcher(bodyString)
        if (matcher.find()) {
            return matcher.group(1).trim()
        }
        return ""
    }

    fun checkErrorSQLException(text: String): String? {
        if (text.isNullOrEmpty()) return null
//        println("开始检查 checkErrorSQLException ")
        val cleanedText = text.replace("\\n|\\r|\\r\\n".toRegex(), "")
        for (rule in configs.ERROR_SYNTAX) {
            val pattern = Pattern.compile(rule, Pattern.CASE_INSENSITIVE)
            if (pattern.matcher(cleanedText).find()) {
                return rule
            }
        }
        return ""
    }

    /**
     * 处理流量是否为可值得扫描的
     *  @param 1. 不为OPTIONS
     *  @param 2. responseTYpe值得扫描
     *  @param 3. 参数个数不超出范围
     */
    fun isRequestAllowed(httpResponseReceived: HttpResponseReceived): Boolean {

        val originalRequest = httpResponseReceived.initiatingRequest()
        val httpRequestMethod = originalRequest.method().equals("OPTIONS")

        if (!isAllowedRequestFileExtension(originalRequest) && !isAllowedRegexURLs(originalRequest.path())) {
            return false
        }
        if (!httpRequestMethod &&
            isAllowedResponseType(httpResponseReceived)
            && isAllowedParamsCounts(originalRequest)
            && isAllowedResponseStatus(httpResponseReceived)
            && checkConfigsChoseBox(httpResponseReceived)
        ) {
            // 判断请求中不仅仅是包含Cookie
            val hasAnyParams = originalRequest.hasParameters()
            val onlyCookies = !originalRequest.hasParameters(HttpParameterType.URL) &&
                    !originalRequest.hasParameters(HttpParameterType.BODY) && !originalRequest.hasParameters(
                HttpParameterType.JSON
            ) && !originalRequest.hasParameters(HttpParameterType.XML_ATTRIBUTE)
                    && !originalRequest.hasParameters(HttpParameterType.XML)
                    && !originalRequest.hasParameters(HttpParameterType.MULTIPART_ATTRIBUTE)
            originalRequest.hasParameters(HttpParameterType.COOKIE)

            return hasAnyParams && !onlyCookies
        }
        return false
    }

    private fun isAllowedRegexURLs(url: String): Boolean {
        return Regex(configs.neverScanRegex, RegexOption.IGNORE_CASE).containsMatchIn(url)
    }

    private fun isAllowedResponseType(response: HttpResponse): Boolean {
        return !configs.allowedMimeTypeMimeType.none { it == response.mimeType().toString() }
    }

    private fun isAllowedRequestFileExtension(request: HttpRequest): Boolean {
        return configs.uninterestingType.none { request.fileExtension().equals(it) }
    }

    private fun isAllowedResponseStatus(response: HttpResponse): Boolean {
        return response.statusCode().toString() == "200"
    }

    private fun checkConfigsChoseBox(responseReceived: HttpResponseReceived): Boolean {

        // 必须开启 startUP 才能继续
        if (!configs.startUP) {
            return false
        }
        // 获取原始请求对象
        val originalRequest = responseReceived.initiatingRequest()
        // 检查 isInScope
        if (configs.isInScope && !originalRequest.isInScope) {
            return false
        }
        // 检查 proxy 和 repeater 条件
        val isProxyConditionMet = configs.proxy && responseReceived.toolSource().isFromTool(ToolType.PROXY)
        val isRepeaterConditionMet = configs.repeater && responseReceived.toolSource().isFromTool(ToolType.REPEATER)
        // 如果选择了 proxy 或 repeater，但对应的条件不满足，则不允许通过
        if ((configs.proxy || configs.repeater) && !(isProxyConditionMet || isRepeaterConditionMet)) {
            return false
        }
        // 如果没有选择 proxy 和 repeater，并且请求来源不是从这两个工具之一，则不允许通过
        if (!(configs.proxy || configs.repeater) &&
            (responseReceived.toolSource().isFromTool(ToolType.PROXY) ||
                    responseReceived.toolSource().isFromTool(ToolType.REPEATER))
        ) return false

        // 如果所有条件都满足，则允许通过
        return true
    }

    private fun isAllowedParamsCounts(request: HttpRequest): Boolean {
        return  request.parameters().count{it.type() != HttpParameterType.COOKIE} <= configs.maxAllowedParameterCount
    }

    fun markerResponseDifferent(
        response1: HttpResponse,
        response2: HttpResponse,
    ): String {
        val oldSize = response1.bodyToString().length
        val newSize = response2.bodyToString().length
        return when {
            oldSize == newSize -> "same"
            newSize > oldSize -> "+${newSize - oldSize}"
            else -> "- ${oldSize - newSize}"
        }
    }

    fun markerTimeStatus(payload: String, requestResponse: HttpRequestResponse): String {
        var responseTime = (requestResponse.timingData().get()
            .timeBetweenRequestSentAndEndOfResponse()
            .toMillis() / 1000.0).toString()
        if (payload.contains("sleep", true)) {
            if (responseTime > payload.split(
                    "sleep(",
                    ignoreCase = true
                )[1].split(")")[0]
            ) {
                println(
                    " \"${responseTime}\"  ${
                        responseTime > payload.split(
                            "sleep(",
                            ignoreCase = true
                        )[1].split(")")[0]
                    }"
                )
                responseTime = "🤔 " + responseTime
                println("存在时间注入！")
                return responseTime
            }
        }
        return responseTime
    }


    /**
     * @param httpResponse
     * @param match
     *
     * Providing an MarkedResponse to highlight relevant portions of requests and responses,
     */
    fun setResponseHighlights(httpResponse: HttpResponse, match: String): MutableList<Marker> {
        val highlights: MutableList<Marker> = mutableListOf()
        val response = httpResponse.toString()
        val regex = Regex(match)
        var start = 0
        regex.findAll(response).forEach { matchResult ->
            val matchStart = matchResult.range.first
            val matchEnd = matchResult.range.last + 1 // Make the end exclusive

            val marker = Marker.marker(matchStart, matchEnd)
            highlights.add(marker)
            start = matchEnd
        }
        return highlights
    }


    fun findResponseDifferences(text1: String, text2: String): Triple<String, String, List<Pair<Int, Int>>> {

        val diff1 = StringBuilder()
        val diff2 = StringBuilder()
        val diffLocations = mutableListOf<Pair<Int, Int>>()

        var i = 0
        var j = 0
        var diffStart = -1

        while (i < text1.length || j < text2.length) {
            when {
                i >= text1.length -> {
                    // text2 has extra characters
                    if (diffStart == -1) diffStart = i
                    diff1.append("-")
                    diff2.append(text2[j])
                    j++
                }

                j >= text2.length -> {
                    // text1 has extra characters
                    if (diffStart == -1) diffStart = i
                    diff1.append(text1[i])
                    diff2.append("-")
                    i++
                }

                text1[i] == text2[j] -> {
                    if (diffStart != -1) {
                        diffLocations.add(Pair(diffStart, i))
                        diffStart = -1
                    }
                    diff1.append(text1[i])
                    diff2.append(text2[j])
                    i++
                    j++
                }

                else -> {
                    // Characters are different
                    if (diffStart == -1) diffStart = i
                    diff1.append(text1[i])
                    diff2.append(text2[j])
                    i++
                    j++
                }
            }
        }

        if (diffStart != -1) {
            diffLocations.add(Pair(diffStart, maxOf(text1.length, text2.length)))
        }

        return Triple(diff1.toString(), diff2.toString(), diffLocations)
    }


    fun <T> parameterValueIsInteger(value: T): Boolean {

        return when (value) {
            is Int, is Long -> true
            is String -> value.all { it.isDigit() }
            else -> false
        }
    }

    fun <T> parameterValueIsBoolean(value: T): Boolean {
        return when (value) {
            is Boolean -> true
            is String -> value.equals("true", ignoreCase = true) || value.equals("false", ignoreCase = true)
            else -> false
        }
    }

    /**
     *  对json内的value数据类型检查
     */
    fun jsonValueType(request: HttpRequest, param: ParsedHttpParameter): Any? {

        val requestString = request.toString()
        val startIndex = param.valueOffsets().startIndexInclusive()
        val endIndex = param.valueOffsets().endIndexExclusive()

        // 检查索引是否有效
        if (startIndex < 0 || endIndex > requestString.length || startIndex >= endIndex) {
            println("Invalid offsets for parameter ${param.name()}")
            return null
        }

        val valueString = requestString.substring(startIndex, endIndex).trim()

        val value: Any? = when {
            valueString == "null" -> null
            startIndex > 0 && endIndex < requestString.length &&
                    requestString[startIndex - 1] == '"' &&
                    requestString[endIndex] == '"' -> valueString // 检查是否被双引号包围
            valueString.toIntOrNull() != null -> valueString.toInt()
            else -> valueString
        }

        /** when (value) {
        is Int -> println("Key: ${param.name()}, Value: $value, Type: Int")
        is String -> println("Key: ${param.name()}, Value: $value, Type: String")
        null -> println("Key: ${param.name()}, Value: $value, Type: Null")
        else -> println("Key: ${param.name()}, Value: $value, Type: Unknown")
        }**/
        return when (value) {

            is Int -> Int
            is String -> String
            else -> null
        }

    }

    /**
     * 在HttpRequest的JOSN数据中，将传入参数的值修改为null
     * @return HttpRequest
     */
    fun replaceJsonParameterValueWithNull(request: HttpRequest, parameter: ParsedHttpParameter): HttpRequest {
        var requestAsString = request.toString()
        val originalStart = parameter.valueOffsets().startIndexInclusive()
        val originalEnd = parameter.valueOffsets().endIndexExclusive()
        val isQuoted = isQuotedValue(requestAsString, originalStart, originalEnd)
        when (parameter.type()) {
            HttpParameterType.JSON -> {
                if (isQuoted) {
                    // 对于带引号的值，从引号前开始替换，完全替换掉带引号的值
                    requestAsString = requestAsString.substring(0, originalStart - 1) +
                            "null" +
                            requestAsString.substring(originalEnd + 1)
                } else {
                    // 对于不带引号的值，直接替换值部分
                    requestAsString = requestAsString.substring(0, originalStart) +
                            "null" +
                            requestAsString.substring(originalEnd)
                }
            }

            else -> {
                // 非JSON参数直接替换值部分
                requestAsString = requestAsString.substring(0, originalStart) +
                        "null" +
                        requestAsString.substring(originalEnd)
            }
        }
        return HttpRequest.httpRequest(request.httpService(), requestAsString).withUpdatedContentLength(true)
    }
    fun replaceAllParameterValuesWithNull(request: HttpRequest, parameters: List<ParsedHttpParameter>): HttpRequest {
        val parameterPositions = parameters.map { param ->
            Triple(
                param,
                param.valueOffsets().startIndexInclusive(),
                param.valueOffsets().endIndexExclusive()
            )
        }.sortedByDescending { it.second }

        var requestAsString = request.toString()

        for ((param, originalStart, originalEnd) in parameterPositions) {
            // 检查参数名是否是分页或大小相关参数
            if (isParameterNameSpecial(param.name())) {
                continue
            }

            val isQuoted = isQuotedValue(requestAsString, originalStart, originalEnd)
            when (param.type()) {
                HttpParameterType.JSON -> {
                    if (isQuoted) {
                        // 对于带引号的值，从引号前开始替换，完全替换掉带引号的值
                        requestAsString = requestAsString.substring(0, originalStart - 1) +
                                "null" +
                                requestAsString.substring(originalEnd + 1)
                    } else {
                        // 对于不带引号的值，直接替换值部分
                        requestAsString = requestAsString.substring(0, originalStart) +
                                "null" +
                                requestAsString.substring(originalEnd)
                    }
                }

                else -> {
                    // 非JSON参数直接替换值部分
                    requestAsString = requestAsString.substring(0, originalStart) +
                            "null" +
                            requestAsString.substring(originalEnd)
                }
            }
        }

        return HttpRequest.httpRequest(request.httpService(), requestAsString)
            .withUpdatedContentLength(true)
    }

    // 检查值是否被引号包围
    private fun isQuotedValue(requestString: String, start: Int, end: Int): Boolean {
        val beforeChar = if (start > 0) requestString[start - 1] else ' '
        val afterChar = if (end < requestString.length) requestString[end] else ' '
        return beforeChar == '"' && afterChar == '"'
    }

    // 检查参数名称是否为特殊参数，如分页或大小相关参数
    private fun isParameterNameSpecial(parameterName: String): Boolean {
        val keywords = listOf(
            "page",
            "num",
            "size",
            "limit"
        )
        return keywords.any { keyword -> parameterName.contains(keyword, ignoreCase = true) }
    }

    fun calculateMD5(input: String): String {
//        val url = requestResponse.request().url().toString()
//        val body = requestResponse.request().body()
//        val inputString = "$url$body"
        val md = MessageDigest.getInstance("MD5")
        val messageDigest = md.digest(input.toByteArray())
        return BigInteger(1, messageDigest).toString(16).padStart(32, '0')
    }

    fun isDuplicate(newHash: String, knownHashes: List<String>): Boolean {
        for (knownHash in knownHashes) {
            if (calculateSimilarity(newHash, knownHash) > 0.9) {
                return true
            }
        }
        return false
    }

    private fun calculateSimilarity(hash1: String, hash2: String): Double {
        require(hash1.length == hash2.length) { "Hash lengths must be equal" }
        var hammingDistance = 0
        for (i in hash1.indices) {
            if (hash1[i] != hash2[i]) {
                hammingDistance++
            }
        }
        return 1.0 - hammingDistance.toDouble() / hash1.length
    }

    /**
     * 查找并标记两个响应之间的差异，返回带标记的HttpRequestResponse
     */
    fun createMarkedRequestResponse(
        originalRequestResponse: HttpRequestResponse,
        newRequestResponse: HttpRequestResponse,
    ): HttpRequestResponse {
        val originalResponse = originalRequestResponse.response().toString()
        val newResponse = newRequestResponse.response().toString()
        val highlights = mutableListOf<Marker>()

        // 查找差异
        var start = 0
        while (start < newResponse.length) {
            // 找到第一个不同的字符位置
            while (start < newResponse.length &&
                start < originalResponse.length &&
                newResponse[start] == originalResponse[start]
            ) {
                start++
            }

            if (start >= newResponse.length) break

            // 找到不同部分的结束位置
            var end = start + 1
            while (end < newResponse.length &&
                (end >= originalResponse.length ||
                        newResponse[end] != originalResponse[end])
            ) {
                end++
            }

            // 添加标记
            if (end > start) {
                highlights.add(Marker.marker(start, end))
            }

            start = end
        }

        // 如果找到差异，创建带标记的请求响应
        return if (highlights.isNotEmpty()) {
            newRequestResponse.withResponseMarkers(highlights)
        } else {
            newRequestResponse
        }
    }

    /**
     * 查找两个响应之间的最大差异部分
     */
    fun findLargestDifference(
        originalResponse: HttpResponse,
        newResponse: HttpResponse,
    ): String? {
        val originalText = originalResponse.body().toString()
        val newText = newResponse.body().toString()
        var largestDiff: String? = null
        var maxLength = 0

        var start = 0
        while (start < newText.length) {
            // 找到第一个不同的字符位置
            while (start < newText.length &&
                start < originalText.length &&
                newText[start] == originalText[start]
            ) {
                start++
            }

            if (start >= newText.length) break

            // 找到不同部分的结束位置
            var end = start + 1
            while (end < newText.length &&
                (end >= originalText.length ||
                        newText[end] != originalText[end])
            ) {
                end++
            }

            // 更新最大差异
            val diffLength = end - start
            if (diffLength > maxLength) {
                maxLength = diffLength
                largestDiff = newText.substring(start, end)
            }

            start = end
        }

        return largestDiff?.dropLast(1)
    }

    /**
     * 在处理响应时使用这个方法来获取差异并标记状态
     */
    fun processResponseWithDifference(
        logs: LogEntry,
        md5: String,
        httpRequestResponse: HttpRequestResponse,
        parameter: String,
        payload: String,
        checkSQL: String?,
    ): Pair<ModifiedLogDataModel, String?> {
        val response = httpRequestResponse.response()
        //取原始response
        val originalRequestResponse = logs.getEntry(md5)?.requestResponse
            ?: return Pair(
                ModifiedLogDataModel(
                    originIndex = md5,
                    parameter = parameter,
                    payload = payload,
                    diff = "Error: Original response not found",
                    status = response.statusCode(),
                    httpRequestResponse = httpRequestResponse,
                    time = markerTimeStatus(payload, httpRequestResponse)
                ),
                null
            )


        // 找出最大的差异部分
//        val diffText = findLargestDifference(
//            originalRequestResponse.response(),
//            httpRequestResponse.response()
//        )

//        val original = Files.readAllLines(File("/Users/javeley/0.txt").toPath())
//        val revised = Files.readAllLines(File("/Users/javeley/1.tx").toPath())


//compute the patch: this is the diffutils part
//        val patch: Patch<Any>? = DiffUtils.diff( originalRequestResponse.response().body().toList(),   httpRequestResponse.response().body().toList())


        val modifiedEntry = ModifiedLogDataModel(
            originIndex = md5,
            parameter = parameter,
            payload = payload,
            diff = if (!checkSQL.isNullOrEmpty()) "Error" else markerResponseDifferent(
                originalRequestResponse.response(),
                response
            ),
            status = response.statusCode(),
            httpRequestResponse = httpRequestResponse,
            time = markerTimeStatus(payload, httpRequestResponse)
        )

        return Pair(modifiedEntry, "")
    }


}

fun main() {

}

