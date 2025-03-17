package processor

import burp.api.montoya.http.message.HttpRequestResponse
import config.Configs

object HiddenParamsProcess {

    private fun getHiddenParams(): MutableList<String> {
        return Configs.INSTANCE.hiddenParams
    }

    fun generateRequest():MutableList<HttpRequestResponse>{
        var requestListWithPayload = mutableListOf<HttpRequestResponse>()
        val hiddenParams = getHiddenParams()
        if (hiddenParams.isNotEmpty()){
            hiddenParams.forEach { params ->
                params
            }
        }


        return requestListWithPayload
    }
}