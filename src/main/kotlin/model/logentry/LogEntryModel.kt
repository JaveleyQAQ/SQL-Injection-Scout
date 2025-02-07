package model.logentry

import burp.api.montoya.http.message.HttpRequestResponse
import utils.RequestResponseUtils

data class LogEntryModel(
    val id: Int,
    val parametersMD5: String,
    val requestResponse: HttpRequestResponse,
    var isChecked: Boolean,
    val method: String = requestResponse.request().method(),
    val host: String = requestResponse.request().headerValue("Host") ?: "",
    val path: String = requestResponse.request().path(),
    val title: String = RequestResponseUtils().getResponseTile(requestResponse.response().body().bytes),
    val status: Short = requestResponse.response().statusCode(),
    val bodyLength: Int = requestResponse.response().body().length(),
    val mimeType: String = requestResponse.response().mimeType().toString(),
    var hasVulnerability: Boolean = false,
    val modifiedEntries: MutableList<ModifiedLogDataModel> = mutableListOf(),
    var interesting:Boolean = false,
    var comments: String? = null
)