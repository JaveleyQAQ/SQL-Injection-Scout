package processor.helper.color

import java.awt.Color


object ColorSortHelper {

    internal fun parseDiffValue(diff: String): Int = when {
        diff == "Error" -> Int.MIN_VALUE
        diff == "same" -> Int.MAX_VALUE

        else -> {
            val cleanStr = diff.trim().removePrefix("+").removePrefix("-").trim()
            cleanStr.toIntOrNull() ?: Int.MAX_VALUE
        }
    }

    internal fun getColorPriority(color: Color?): Int = when (color) {
        Color.RED -> 0        // 红色最优先
        Color.ORANGE -> 1     // 橙色
        Color.YELLOW -> 2     // 黄色
        Color.GREEN -> 3      // 绿色
        else -> 99
    }
}