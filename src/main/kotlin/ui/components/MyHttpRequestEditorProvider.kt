package ui.components

import burp.api.montoya.MontoyaApi
import burp.api.montoya.ui.editor.extension.EditorCreationContext
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor
import burp.api.montoya.ui.editor.extension.HttpRequestEditorProvider


//internal class MyHttpRequestEditorProvider(private val api: MontoyaApi) : HttpRequestEditorProvider {
//    override fun provideHttpRequestEditor(creationContext: EditorCreationContext): ExtensionProvidedHttpRequestEditor {
//        return MyExtensionProverHtppRequestsEditor(api, creationContext)
//    }
//}