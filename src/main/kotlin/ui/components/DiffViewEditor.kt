import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.ToolType
import burp.api.montoya.http.message.HttpRequestResponse
import burp.api.montoya.http.message.responses.HttpResponse
import burp.api.montoya.ui.Selection
import burp.api.montoya.ui.editor.extension.EditorCreationContext
import burp.api.montoya.ui.editor.extension.EditorMode
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpResponseEditor
import burp.api.montoya.ui.editor.extension.HttpResponseEditorProvider
import model.logentry.ModifiedLogEntry
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rtextarea.RTextScrollPane
import ui.components.DiffViewEditPanel
import java.awt.Component

class DiffViewEditor(val api: MontoyaApi, private val modifiedLogEntry: ModifiedLogEntry) : HttpResponseEditorProvider {
    override fun provideHttpResponseEditor(creationContext: EditorCreationContext?): ExtensionProvidedHttpResponseEditor {
        return DiffEditor(api, modifiedLogEntry, creationContext)
    }

    class DiffEditor(
        val api: MontoyaApi,
        val modifiedLogEntry: ModifiedLogEntry,
        val creationContext: EditorCreationContext?,
    ) : ExtensionProvidedHttpResponseEditor {
//        private var textArea: RawEditor = api.userInterface().createRawEditor()
        private lateinit var response: HttpResponse
        private val diffViewEditPanel = DiffViewEditPanel()
        //= JTextArea()
        private val textArea = RSyntaxTextArea(20, 30)
        private lateinit var sp:RTextScrollPane

        init {
            if (creationContext?.editorMode() == EditorMode.READ_ONLY && creationContext.toolSource()
                    .isFromTool(ToolType.EXTENSIONS)
            ) {

                textArea.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_JAVA
                textArea.isCodeFoldingEnabled = true
                textArea.isAutoIndentEnabled = true
                textArea.matchedBracketBorderColor
                textArea.lineWrap = true // 启用自动换行
                textArea.wrapStyleWord = true // 按单词换行，而不是在字符处换行
                sp = RTextScrollPane(textArea)


//                    api.userInterface().createRawEditor()

            }
        }

        override fun setRequestResponse(requestResponse: HttpRequestResponse?) {
            this.response = requestResponse?.response()!!
            textArea.text = " "
            val originalText  = response.bodyToString()
            val revisedText  = modifiedLogEntry.getModifiedEntry(
                modifiedLogEntry.getCurrentMD5(),
                modifiedLogEntry.getCurrentRow()
            )?.httpRequestResponse!!.response().bodyToString()
            val sections = listOf(
                Triple("Response Body", originalText, revisedText)
            )
            diffViewEditPanel.displayDiff(sections)
//            textArea.text = modifiedLogEntry.getModifiedEntry(
//                modifiedLogEntry.getCurrentMD5(),
//                modifiedLogEntry.getCurrentRow()
//            )?.diffString


//            textArea.contents = ByteArray.byteArray("")
//            val a = modifiedLogEntry.getModifiedEntry(
//                modifiedLogEntry.getCurrentMD5(),
//                modifiedLogEntry.getCurrentRow()
//            )?.diffString
//            textArea.contents = ByteArray.byteArray(a)



        }

        override fun isEnabledFor(requestResponse: HttpRequestResponse?): Boolean {
            return true
        }

        override fun caption(): String {
            return "Diff"
        }

        override fun uiComponent(): Component {
            return diffViewEditPanel
        }

        override fun selectedData(): Selection? {

            return  null
//            return if (textArea.selection().isPresent()) textArea.selection().get() else null
        }

        override fun isModified(): Boolean {
            return true
        }

        override fun getResponse(): HttpResponse {
            return response
        }

    }


//    class DiffEditor(
//        val api: MontoyaApi,
//        val modifiedLogEntry: ModifiedLogEntry,
//        creationContext: EditorCreationContext?,
//    ) :
//        ExtensionProvidedHttpResponseEditor {
//        private val responseView: RawEditor?
//        private lateinit var httpRequestResponse: HttpRequestResponse
//        private var jTextArea: JTextArea = JTextArea()
//
//        init {
//            if (creationContext?.editorMode() != EditorMode.READ_ONLY) {
//                responseView = api.userInterface().createRawEditor(EditorOptions.READ_ONLY)
//
//            } else {
//                responseView = null
//            }
//
//        }
//
//        override fun setRequestResponse(requestResponse: HttpRequestResponse?) {
//            jTextArea.text = ""
//            jTextArea.text = modifiedLogEntry.getModifiedEntry(modifiedLogEntry.getCurrentMD5(), 1)?.diffString
//
//        }
//
//        override fun isEnabledFor(requestResponse: HttpRequestResponse?): Boolean {
//            return true
//
//        }
//
//        override fun caption(): String {
//            return "diff"
//        }
//
//        override fun uiComponent(): Component {
//            return jTextArea
//        }
//
//        override fun selectedData(): Selection? {
//            return if (responseView?.selection()?.isPresent!!) responseView.selection().get() else null
//        }
//
//        override fun isModified(): Boolean {
//            return true
//        }
//
//
//        override fun getResponse(): HttpResponse {
//            TODO("Not yet implemented")
//        }
//
//    }
//
//

}