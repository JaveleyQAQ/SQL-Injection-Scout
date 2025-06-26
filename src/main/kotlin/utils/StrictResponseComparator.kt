//package utils
//
//import kotlinx.serialization.json.*
//import kotlin.math.roundToInt
//
//// JSON数据类型枚举
//enum class JsonType {
//    STRING, NUMBER, BOOLEAN, ARRAY, OBJECT, NULL
//}
//
//// 元数据结构：记录类型和长度
//data class Metadata(
//    val type: JsonType,
//    val length: Int? = null
//)
//
//class JsonComparator {
//    // 提取元数据（类型 + 長度）
//    private fun extractMetadata(json: JsonElement): Map<String, Metadata> {
//        val metadata = mutableMapOf<String, Metadata>()
//        if (json !is JsonObject) return metadata // 非对象类型直接返回空
//
//        json.forEach { (key, value) ->
//            metadata[key] = getValueMetadata(value)
//        }
//        return metadata
//    }
//
//    // 获取单个值的元数据
//    private fun getValueMetadata(element: JsonElement): Metadata {
//        return when (element) {
//            is JsonPrimitive -> {
//                val primitive = element.jsonPrimitive
//                val type = when {
////                    primitive.isBoolean -> utils.JsonType.BOOLEAN // 布尔类型判断
//                    // 替换 isNumber 判断为尝试解析数值类型
//                    primitive.doubleOrNull != null || primitive.intOrNull != null -> JsonType.NUMBER // 数值类型判断 <button class="citation-flag" data-index="5"><button class="citation-flag" data-index="9">
//                    primitive.content != null -> JsonType.STRING // 字符串类型判断 <button class="citation-flag" data-index="9">
//                    else -> JsonType.NULL
//                }
//
//                val length = when (type) {
//                    JsonType.STRING -> primitive.content?.length ?: 0
//                    JsonType.NUMBER -> primitive.content?.length ?: 0 // 数值转字符串计算长度 <button class="citation-flag" data-index="5">
//                    else -> 0
//                }
//
//                Metadata(type, length)
//            }
//            is JsonArray -> Metadata(JsonType.ARRAY, element.size)
//            is JsonObject -> Metadata(JsonType.OBJECT, element.size)
//            else -> Metadata(JsonType.NULL, null)
//        }
//    }
//
//    // 计算相似度
//    fun calculateSimilarity(originalJson: String, modifiedJson: String): Double {
//        val original = Json.parseToJsonElement(originalJson)
//        val modified = Json.parseToJsonElement(modifiedJson)
//
//        val originalMeta = extractMetadata(original)
//        val modifiedMeta = extractMetadata(modified)
//
//        // 计算相似度分母（总键数）
//        val allKeys = originalMeta.keys + modifiedMeta.keys
//        val total = allKeys.size
//
//        // 计算匹配数
//        var matched = 0
//        for (key in allKeys) {
//            val origMeta = originalMeta[key]
//            val modMeta = modifiedMeta[key]
//
//            if (origMeta != null && modMeta != null) {
//                if (origMeta.type == modMeta.type &&
//                    origMeta.length == modMeta.length) {
//                    matched++
//                }
//            }
//        }
//
//        return (matched.toDouble() / total * 100).roundToInt().toDouble()
//    }
//}
//
//// 测试入口
//fun main() {
//    val comparator = JsonComparator()
//
//    // 示例1：完全匹配（类型和长度一致）
//    val original1 = """
//        {"error":"0","logid":"3721474359993651900"}
//                """
//        .trimIndent()
//    val modified1 = """
//        {"error":"0","logid":"443471180450627723"}
//                """
//        .trimIndent()
//
//    println("相似度：${comparator.calculateSimilarity(original1, modified1)}%") // 输出 100%、
//    // 示例2：长度变化（"q"值变长）
//    val modified2 = """{"error":"0","logid":"1", "A":"B"}  """
//    println("相似度：${comparator.calculateSimilarity(original1, modified2)}%") // 输出 ~66.6%
//    // 示例3：类型变化（数字转字符串）
//    val original2 = """{"price": 123.45}"""
//
//    val modified3 = """{"price": "我草泥马的"}"""
//    println("相似度：${comparator.calculateSimilarity(original2, modified3)}%") // 输出 0%（类型不同）
//}