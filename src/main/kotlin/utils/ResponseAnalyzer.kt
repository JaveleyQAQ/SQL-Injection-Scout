package utils
//class ResponseAnalyzer {
//    fun analyzeSearchResponse(original: String, injected: String): Boolean {
//        // 1. 移除搜索词相关的动态内容
//        val cleanOriginal = removeSearchTermContent(original, originalTerm)
//        val cleanInjected = removeSearchTermContent(injected, injectedTerm)
//
//        // 2. 结构相似度分析
//        val structuralSimilarity = compareStructure(cleanOriginal, cleanInjected)
//
//        // 3. 内容模式分析
//        val patternSimilarity = compareContentPattern(cleanOriginal, cleanInjected)
//
//        return structuralSimilarity > 0.9 && patternSimilarity > 0.8
//    }
//
//    private fun compareStructure(str1: String, str2: String): Double {
//        // 提取HTML/JSON结构特征
//        val structure1 = extractStructurePattern(str1)
//        val structure2 = extractStructurePattern(str2)
//
//        // 计算结构相似度
//        return calculateSimilarity(structure1, structure2)
//    }
//
//    private fun compareContentPattern(str1: String, str2: String): Double {
//        // 提取内容模式（如分页信息、结果数量等）
//        val pattern1 = extractContentPattern(str1)
//        val pattern2 = extractContentPattern(str2)
//
//        return calculatePatternSimilarity(pattern1, pattern2)
//    }
//
//    private fun removeSearchTermContent(content: String, term: String): String {
//        return content.replace(Regex("""$term"""), "")
//    }
//
//    private fun extractStructurePattern(content: String): String {
//        // 实现提取HTML/JSON结构特征的逻辑
//        // 这里可以使用正则表达式或其他方法来提取结构特征
//        return ""
//    }
//
//    private fun extractContentPattern(content: String): String {
//        // 实现提取内容模式的逻辑
//        // 这里可以使用正则表达式或其他方法来提取内容模式
//        return ""
//    }
//
//    private fun calculatePatternSimilarity(pattern1: String, pattern2: String): Double {
//        // 实现计算内容模式相似度的逻辑
//        // 这里可以使用其他方法来计算内容模式相似度
//        return 0.0
//    }
//
//    private fun calculateSimilarity(str1: String, str2: String): Double {
//        // 使用Levenshtein距离计算相似度
//        val distance = levenshteinDistance(str1, str2)
//        return 1 - (distance.toDouble() / maxOf(str1.length, str2.length))
//    }
//}