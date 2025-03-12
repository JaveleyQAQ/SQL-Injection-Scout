package processor.helper.color

import java.awt.Color

object ColorSortHelper {
    internal fun parseDiffValue(diff: String): Int = when {
        diff == "Error" -> Int.MIN_VALUE
        diff == "same" -> Int.MAX_VALUE
        else -> diff.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
    }

    internal fun getColorPriority(color: Color?): Int? = when (color) {
        Color.RED -> 0
        Color.YELLOW -> 1
        Color.GREEN -> 2
        null -> 3
        else -> null // 灰色不参与排序
    }
}