package processor.helper.color

import java.awt.Color

object ColorManager {

    fun determineColor(diff: String, payloadLength: Int, responseCode: Int, diffCount: Int): List<Color?> {
        return when {
            diff == "Error" ->
                listOf(Color.RED, Color.BLACK)

            diff == "same" || diffCount >= 6 ->
                listOf(Color.LIGHT_GRAY, Color.BLACK)

            when (responseCode) {
                200,301,302,500, 501, 502, 503, 504, 505 -> true
                else -> false } -> listOf(Color.GREEN, Color.BLACK)

            // 带 + / - 的内容变化
            diff.startsWith("+") || diff.startsWith("-") -> {
                val diffValue = diff.substring(1).toIntOrNull() ?: 0
                if (diffValue != 0 && diffValue != payloadLength) {
                    listOf(Color.GREEN, Color.BLACK)
                } else {
                    listOf(Color.LIGHT_GRAY, Color.BLACK)
                }
            }
            else ->
                listOf(Color.LIGHT_GRAY, Color.BLACK)
        }
    }
}