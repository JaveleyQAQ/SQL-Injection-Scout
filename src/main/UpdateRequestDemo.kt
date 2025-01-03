interface HttpRequest {
    fun httpService(): String // 假设有一个获取服务的方法
    fun parameters(): List<ParsedHttpParameter>
    fun updateParameterValue(parsedParameter: ParsedHttpParameter, encodedValue: String, payloadUpdateMode: PayloadUpdateMode): HttpRequest
}

data class ParsedHttpParameter(
    val name: String,
    val type: String,
    val value: String,
    val valueOffsets: ValueOffsets
)

data class ValueOffsets(val startIndexInclusive: Int, val endIndexExclusive: Int)


enum class PayloadUpdateMode {
    REPLACE,
    PREPEND,
    INSERT_MIDDLE,
    APPEND
}

public fun HttpRequest.withUpdatedParsedParameterValue(
    parsedParameter: ParsedHttpParameter,
    encodedValue: String,
    payloadUpdateMode: PayloadUpdateMode = PayloadUpdateMode.REPLACE
): HttpRequest {
    val updatedParsedParam = this.parameters().find {
        it.name == parsedParameter.name && it.type == parsedParameter.type && it.value == parsedParameter.value
    }

    return if (updatedParsedParam != null) {
        this.updateParameterValue(updatedParsedParam, encodedValue, payloadUpdateMode)
    } else {
        this
    }
}

// 假设 HttpRequest 有以下方法
class SimpleHttpRequest(val service: String, val params: MutableList<ParsedHttpParameter>) : HttpRequest {
    override fun httpService(): String = service

    override fun parameters(): List<ParsedHttpParameter> = params

    override fun updateParameterValue(
        parsedParameter: ParsedHttpParameter,
        encodedValue: String,
        payloadUpdateMode: PayloadUpdateMode
    ): HttpRequest {
        val index = params.indexOf(parsedParameter)
        if(index == -1) return this
        val old = params[index]

        val newValue = when(payloadUpdateMode) {
            PayloadUpdateMode.PREPEND -> encodedValue + old.value
            PayloadUpdateMode.APPEND -> old.value + encodedValue
            PayloadUpdateMode.INSERT_MIDDLE -> {
                val middleIndexDiff = old.value.length/2
                if (middleIndexDiff > 0) {
                    old.value.substring(0, middleIndexDiff) + encodedValue + old.value.substring(middleIndexDiff)
                } else {
                    encodedValue + old.value
                }
            }

            else -> encodedValue

        }

        params[index] = old.copy(value = newValue)
        return this
    }

    override fun toString() : String {
        return "SimpleHttpRequest(service='$service', params=$params)"
    }

}