package utils

import burp.api.montoya.http.message.params.HttpParameter
import burp.api.montoya.http.message.params.HttpParameterType
import burp.api.montoya.http.message.params.ParsedHttpParameter
import burp.api.montoya.http.message.requests.HttpRequest


public fun HttpRequest.withUpdatedContentLength(addContentLengthHeaderIfNotPresent : Boolean = false) : HttpRequest {
    if(this.hasHeader("Content-Length"))
        return this.withUpdatedHeader("Content-Length",this.body().length().toString())
    else if(addContentLengthHeaderIfNotPresent)
        return this.withAddedHeader("Content-Length",this.body().length().toString())
    return this
}

enum class PayloadUpdateMode {
    REPLACE,
    PREPEND,
    INSERT_MIDDLE,
    APPEND
}

public fun HttpRequest.withUpdatedParsedParameterValue(parsedParameter : ParsedHttpParameter, encodedValue : String, payloadUpdateMode : PayloadUpdateMode = PayloadUpdateMode.REPLACE) : HttpRequest
{
    var updatedParsedParam = this.parameters().find { it.name()==parsedParameter.name() && it.type() == parsedParameter.type() && it.value()==parsedParameter.value() }

    if(updatedParsedParam!=null) {
        val requestAsString = this.toString()

        when (payloadUpdateMode) {
            PayloadUpdateMode.PREPEND -> {
                val part1 = requestAsString.substring(0, updatedParsedParam.valueOffsets().startIndexInclusive())
                val part2 = requestAsString.substring(updatedParsedParam.valueOffsets().startIndexInclusive() + 1)
                return HttpRequest.httpRequest(this.httpService(), part1 + encodedValue + part2)
            }

            PayloadUpdateMode.INSERT_MIDDLE -> {
                val middleIndexDiff =
                    (updatedParsedParam.valueOffsets().endIndexExclusive() - updatedParsedParam.valueOffsets()
                        .startIndexInclusive()) / 2
                if (middleIndexDiff > 1) {
                    val part1 = requestAsString.substring(
                        0,
                        updatedParsedParam.valueOffsets().startIndexInclusive() + middleIndexDiff
                    )
                    val part2 = requestAsString.substring(
                        updatedParsedParam.valueOffsets().startIndexInclusive() + middleIndexDiff + 1
                    )
                    return HttpRequest.httpRequest(this.httpService(), part1 + encodedValue + part2)
                }
                val part1 = requestAsString.substring(0, updatedParsedParam.valueOffsets().startIndexInclusive())
                val part2 = requestAsString.substring(updatedParsedParam.valueOffsets().startIndexInclusive() + 1)
                return HttpRequest.httpRequest(this.httpService(), part1 + encodedValue + part2)
            }

            PayloadUpdateMode.APPEND -> {
                val part1 = requestAsString.substring(0, updatedParsedParam.valueOffsets().endIndexExclusive())
                val part2 = requestAsString.substring(updatedParsedParam.valueOffsets().endIndexExclusive())
                return HttpRequest.httpRequest(this.httpService(), part1 + encodedValue + part2)
            }

            else -> {
                val part1 = requestAsString.substring(0, updatedParsedParam.valueOffsets().startIndexInclusive())
                val part2 = requestAsString.substring(updatedParsedParam.valueOffsets().endIndexExclusive())
                return HttpRequest.httpRequest(this.httpService(), part1 + encodedValue + part2)
            }
        }
    }
    return this
}

// This method patches the https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/http/message/requests/HttpRequest.html interface to include a new "addOrUpdateHeader(header,value)" method.
public fun HttpRequest.withAddedOrUpdatedHeader(headerName : String, headerValue : String) : HttpRequest {

    return if(this.hasHeader(headerName))
        this.withUpdatedHeader(headerName, headerValue)
    else
        this.withAddedHeader(headerName, headerValue)
}

// This method patches the https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/http/message/requests/HttpRequest.html interface to include a new "addOrUpdateCookie(cookie,value)" method.
public fun HttpRequest.withAddedOrUpdatedCookie(cookieName : String, cookieValue : String) : HttpRequest {

    return if(this.hasParameter("token", HttpParameterType.COOKIE)) {
        this.withUpdatedParameters(HttpParameter.cookieParameter(cookieName,cookieValue))
    }
    else {
        this.withAddedParameters(HttpParameter.cookieParameter(cookieName,cookieValue))
    }
}