package ui.components

import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.ByteArray
import burp.api.montoya.core.ByteArray.byteArray
import burp.api.montoya.http.message.HttpRequestResponse
import burp.api.montoya.http.message.params.HttpParameter
import burp.api.montoya.http.message.params.ParsedHttpParameter
import burp.api.montoya.http.message.requests.HttpRequest
import burp.api.montoya.ui.Selection
import burp.api.montoya.ui.editor.EditorOptions
import burp.api.montoya.ui.editor.RawEditor
import burp.api.montoya.ui.editor.extension.EditorCreationContext
import burp.api.montoya.ui.editor.extension.EditorMode
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor
import burp.api.montoya.utilities.Base64EncodingOptions
import burp.api.montoya.utilities.Base64Utils
import burp.api.montoya.utilities.URLUtils
import java.awt.Component
import java.util.*


class MyExtensionProverHtppRequestsEditor(api: MontoyaApi, creationContext: EditorCreationContext) :
    ExtensionProvidedHttpRequestEditor {

    private val requestEditor: RawEditor
//    private val requestResponse: HttpRequestResponse? = null
    private val base64Utils: Base64Utils = api.utilities().base64Utils()
    private val URLUtils: URLUtils = api.utilities().urlUtils()
    private var parsedHttpParameter: ParsedHttpParameter? = null


    private val httpRequestResponse:HttpRequestResponse?=null

    init {


        if (creationContext.editorMode() == EditorMode.READ_ONLY) {
            requestEditor = api.userInterface().createRawEditor(EditorOptions.READ_ONLY)
        } else {
            requestEditor = api.userInterface().createRawEditor()
        }
    }


    override fun setRequestResponse(p0: HttpRequestResponse?) {
        val urlDecoded: String = this.URLUtils.decode(parsedHttpParameter!!.value())


        var output: Any? = null
        try {
            output = base64Utils.decode(urlDecoded).bytes
        } catch (e: Exception) {
            output = byteArray(urlDecoded).bytes
        }
        this.requestEditor?.contents = output as ByteArray?
    }

    override fun isEnabledFor(requestResponse: HttpRequestResponse): Boolean {
        val dataParam: Optional<ParsedHttpParameter>? = requestResponse.request().parameters().stream().filter { p ->
            p.name().equals("data")
        }.findFirst()

        if (dataParam != null) {
            dataParam.ifPresent { httpParameter: ParsedHttpParameter -> parsedHttpParameter = httpParameter }
        }

        if (dataParam != null) {
            return dataParam.isPresent
        }

        return false
    }

    override fun caption(): String {
        return "Serialized input";
    }

    override fun uiComponent(): Component {
        return requestEditor?.uiComponent()!!
    }

    override fun selectedData(): Selection? {
        return if (requestEditor.selection().isPresent) requestEditor.selection().get() else null
    }

    override fun isModified(): Boolean {
        return requestEditor.isModified();
    }

    override fun getRequest(): HttpRequest {

        var request: HttpRequest



        if (requestEditor?.isModified == true) {
            val encode = base64Utils.encodeToString(requestEditor!!.contents, Base64EncodingOptions.URL)
            val urlEncoder = URLUtils.encode(encode)
            request = httpRequestResponse?.request()?.withUpdatedParameters(
                HttpParameter.parameter(
                    parsedHttpParameter?.name(),
                    encode,
                    parsedHttpParameter?.type()
                )
            )!!

        } else {
            request = httpRequestResponse!!.request()
        }
        return request
    }
}