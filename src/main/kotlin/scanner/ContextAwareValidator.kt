package scanner//import burp.api.montoya.http.message.responses.HttpResponse
//
//class ContextAwareValidator {
//    fun validateInjection(
//        originalResponse: HttpResponse,
//        injectedResponse: HttpResponse,
//        context: InjectionContext
//    ): Boolean {
//        return when(context) {
//            is ErrorContext -> validateErrorBased(originalResponse, injectedResponse)
//            is UnionContext -> validateUnionBased(originalResponse, injectedResponse)
//            is BooleanContext -> validateBooleanBased(originalResponse, injectedResponse)
//            else -> validateGeneric(originalResponse, injectedResponse)
//        }
//    }
//
//    private fun validateErrorBased(original: HttpResponse, injected: HttpResponse): Boolean {
//        // 检查是否包含特定的SQL错误
//        val errorPatterns = listOf(
//            "SQL syntax.*MySQL",
//            "Warning.*mysql_.*",
//            "valid MySQL result",
//            "MySqlClient\\.."
//        ).map { Regex(it, RegexOption.IGNORE_CASE) }
//
//        return errorPatterns.any { pattern ->
//            pattern.containsMatchIn(injected.bodyToString()) &&
//            !pattern.containsMatchIn(original.bodyToString())
//        }
//    }
//}