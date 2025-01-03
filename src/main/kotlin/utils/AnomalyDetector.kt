package utils
//class AnomalyDetector {
//    private val normalPatterns = mapOf(
//        "pagination" to Regex("""第\s*\d+\s*页"""),
//        "resultCount" to Regex("""共?\s*\d+\s*条?结果"""),
//        "searchTerm" to Regex("""搜索[：:]\s*["'].*?["']""")
//    )
//
//    fun detectAnomalies(response: String): List<Anomaly> {
//        val anomalies = mutableListOf<Anomaly>()
//
//        // 1. 检测结构异常
//        if (hasStructuralAnomaly(response)) {
//            anomalies.add(Anomaly.STRUCTURAL_ERROR)
//        }
//
//        // 2. 检测内容异常
//        if (hasContentAnomaly(response)) {
//            anomalies.add(Anomaly.CONTENT_ERROR)
//        }
//
//        // 3. 检测错误信息
//        findErrorMessages(response)?.let {
//            anomalies.add(Anomaly.ERROR_MESSAGE)
//        }
//
//        return anomalies
//    }
//
//    private fun hasStructuralAnomaly(response: String): Boolean {
//        // 检查HTML/JSON结构是否完整
//        return !isValidStructure(response)
//    }
//
//    private fun hasContentAnomaly(response: String): Boolean {
//        // 检查是否包含预��的内容模式
//        return !normalPatterns.all { (_, pattern) ->
//            pattern.find(response) != null
//        }
//    }
//}
//
////主要改进点：
////结构化分析：
////分析HTML/JSON结构
////比较DOM树或JSON对象结构
////忽略动态内容的影响
////内容模式识别：
////识别搜索结果的固定模式
////分析分页信息
////提取结果数量信息
////异常检测：
////检测结构异常
////识别错误信息
////分析响应完整性
////上下文感知：
////考虑搜索词的影响
////分析结果相关性
////评估变化的合理性
////这样的改进可以：
////1. 减少因搜索结果自然变化导致的误报
////更准确地识别真实的注入响应
////提高检测的准确性和可靠性
////您觉得这些建议如何？我们可以先实现其中某个部分进行测试/