package model.logentry

import java.awt.Color

class ColorManager {

    fun determineColor(diff: String, payloadLength: Int, responseCode: Int, diffCount: Int): List<Color?> {
        return when {
            diff == "Error" -> listOf(Color.RED, Color.BLACK)  // 添加对Error响应的处理
            diff == "same" -> listOf(Color.LIGHT_GRAY, Color.BLACK)
            responseCode in 400..404 -> listOf(Color.LIGHT_GRAY, Color.BLACK)
            responseCode in 501..505 -> listOf(Color.LIGHT_GRAY, Color.BLACK)
            diffCount >= 7 -> listOf(Color.LIGHT_GRAY, Color.BLACK)  // 频繁出现的差异
            diff.startsWith("+") || diff.startsWith("-") -> {
                val diffValue = diff.substring(1).toIntOrNull() ?: 0
                if (diffValue != payloadLength) {
                    listOf(Color.GREEN, Color.BLACK)  // 有趣的差异
                } else {
                    listOf(Color.LIGHT_GRAY, Color.BLACK)
                }
            }
            else -> listOf(null, null)  // 默认使用灰色
        }
    }
} 