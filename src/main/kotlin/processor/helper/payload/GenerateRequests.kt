package processor.helper.payload

import burp.api.montoya.http.message.ContentType
import burp.api.montoya.http.message.params.HttpParameter
import burp.api.montoya.http.message.params.HttpParameterType
import burp.api.montoya.http.message.params.ParsedHttpParameter
import burp.api.montoya.http.message.requests.HttpRequest
import com.nickcoblentz.montoya.PayloadUpdateMode
import com.nickcoblentz.montoya.withUpdatedContentLength
import com.nickcoblentz.montoya.withUpdatedParsedParameterValue
import config.Configs
import utils.RequestResponseUtils
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

object GenerateRequests {
    private val hiddenParams  = Configs.INSTANCE.hiddenParams
    private val configs = Configs.INSTANCE
    private val requestResponseUtils = RequestResponseUtils()
    private val uniqScannedParameters: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private var requestPayloadMap: MutableMap<HttpRequest?, Pair<String, String>> =
        ConcurrentHashMap() // 记录请求的参数和payload

    fun processRequests(httpRequest: HttpRequest): List<HttpRequest> {
        val newRequest =  addHiddenParamsToHttpRequest(httpRequest)
        return generateRequestByPayload(newRequest,newRequest.parameters(), configs.payloads)
    }





    private fun addHiddenParamsToHttpRequest(httpRequest: HttpRequest): HttpRequest {
        val hiddenHttpParameters:MutableList<HttpParameter> = mutableListOf()
        var paramsType: HttpParameterType? = null
        paramsType = when(httpRequest.contentType()){
            ContentType.JSON -> HttpParameterType.JSON
            ContentType.NONE -> HttpParameterType.URL
            ContentType.XML -> HttpParameterType.XML
            else -> HttpParameterType.URL
        }
        hiddenParams.forEach { params ->
            hiddenHttpParameters.add(HttpParameter.parameter(params, generateRandomString(3), paramsType))
        }
        val newRequest  =httpRequest.withAddedParameters(hiddenHttpParameters)
       return newRequest
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

    fun getRequestPayloadMap(): MutableMap<HttpRequest?, Pair<String, String>> {
        return requestPayloadMap
    }

    fun getUniqScannedParameters(): MutableSet<String> {
        return uniqScannedParameters
    }

    fun cleanData(){
        requestPayloadMap.clear()
        uniqScannedParameters.clear()
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
                    payload.replace("\"", "%22", true).replace("#", "%23", true).
                    replace(" ", "%20", true),
//                    api.utilities().urlUtils().encode(payload),
                    module
                ).withUpdatedContentLength(true)


        }


    }

    private fun generateRandomString(length: Int): String {
        val allowedChars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    private fun generateRandomNumber(length: Int): String {
        val allowedDigits = ('0'..'9')
        return (1..length)
            .map { allowedDigits.random() }
            .joinToString("")
    }
}