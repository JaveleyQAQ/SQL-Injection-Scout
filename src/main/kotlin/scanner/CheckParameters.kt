package scanner

import burp.api.montoya.MontoyaApi
import burp.api.montoya.http.handler.*

class CheckParameters(val api: MontoyaApi):HttpHandler {
//    private val api: MontoyaApi? = null
//private val api: MontoyaApi? = null
    private val configLoader= null
    private val httpUtils = null
    private val messageTableModel = null
    private val messageProcessor = null

    fun checkParameters(request: String): String {
        // Implement parameter checking logic here
//        ProcessMessage

        return "Check parameters for request: $request"


    }

    override fun handleHttpRequestToBeSent(httpRequestToBeSent: HttpRequestToBeSent?): RequestToBeSentAction {
//        httpRequestToBeSent.toolSource()


        TODO("Not yet implemented")
    }

    override fun handleHttpResponseReceived(p0: HttpResponseReceived?): ResponseReceivedAction {
        TODO("Not yet implemented")
    }


}