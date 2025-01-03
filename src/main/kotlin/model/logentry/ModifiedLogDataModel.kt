package model.logentry

import burp.api.montoya.http.message.HttpRequestResponse
import java.awt.Color

data class ModifiedLogDataModel(
    val originIndex: String,
    val parameter:String,
    val payload:String,
    val diff: String,
    val status: Short,
    var vulnerability: Boolean = false,
    val httpRequestResponse: HttpRequestResponse,
    val time: Any,
    var diffString: String? =null,
    var color: List<Color?> = mutableListOf(null, null)
//    var color: Color? = null // mutableListOf(Color.GRAY,Color.WHITE)  //设置默认表单颜色
)
