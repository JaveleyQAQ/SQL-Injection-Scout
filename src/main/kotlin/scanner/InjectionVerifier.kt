package scanner//import burp.api.montoya.MontoyaApi
//import burp.api.montoya.http.message.requests.HttpRequest
//
//class InjectionVerifier(private val api: MontoyaApi) {
//    private val responseAnalyzer = ResponseAnalyzer()
//
//    fun verifyInjection(request: HttpRequest, parameter: String, payload: String): Boolean {
//        // 1. 发送多次原始请求建立基准
//        val baselineResponses = (1..3).map {
//            api.http().sendRequest(request)
//        }
//
//        // 2. 检查基准请求的稳定性
//        if (!isBaselineStable(baselineResponses)) {
//            return false // 基准不稳定，跳过检测
//        }
//
//        // 3. 多次发送注入请求
//        val injectedRequest = injectPayload(request, parameter, payload)
//        val injectionResponses = (1..3).map {
//            api.http().sendRequest(injectedRequest)
//        }
//
//        // 4. 对比响应
//        val differentResponseCount = injectionResponses.count { injectionResponse ->
//            baselineResponses.all { baselineResponse ->
//                responseAnalyzer.isDifferentResponse(baselineResponse, injectionResponse)
//            }
//        }
//
//        // 要求至少2次注入响应都显示差异
//        return differentResponseCount >= 2
//    }
//
//    private fun isBaselineStable(responses: List<HttpResponse>): Boolean {
//        // 检查基准请求的响应是否稳定
//        return responses.zipWithNext().all { (resp1, resp2) ->
//            !responseAnalyzer.isDifferentResponse(resp1, resp2)
//        }
//    }
//}