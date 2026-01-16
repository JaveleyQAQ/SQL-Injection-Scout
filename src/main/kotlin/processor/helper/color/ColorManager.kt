package processor.helper.color

import java.awt.Color
import kotlin.math.abs

object ColorManager {

    private val COLOR_CRITICAL = Color(255, 100, 100) // 浅红：SQL报错
    private val COLOR_INTERESTING = Color.GREEN       // 绿色：有效差异
    private val COLOR_WARNING = Color.ORANGE          // 橙色：服务端异常 (5xx)
    private val COLOR_IGNORE = Color.LIGHT_GRAY       // 灰色：无效/反射/相同
    private val COLOR_TEXT = Color.BLACK              // 黑色：字体颜色

    fun determineColor(diff: String, payloadLength: Int, responseCode: Int, diffCount: Int): List<Color?> {

        // 1. 明确的 SQL 报错
        if (diff == "Error") {
            return listOf(COLOR_CRITICAL, COLOR_TEXT)
        }

        // 2. [去噪逻辑] 完全相同 或 出现频率过高(可能是动态参数)
        // 任何被标记为 same 的，或者同一个 diff 出现超过 6 次的，直接忽略
        if (diff == "same" || diffCount >= 6) {
            return listOf(COLOR_IGNORE, COLOR_TEXT)
        }

        // 3. [核心逻辑] 处理 "+10", "-100" 这种长度差异
        if (diff.startsWith("+") || diff.startsWith("-")) {
            // +/- diff 提取数值
            val diffVal = diff.replace(" ", "").toIntOrNull() ?: 0
            val absDiff = abs(diffVal)

            // 反射检测
            // 如果 响应长度变化值 == Payload 长度
            // 例子：输入 'abc' (len=3)，响应变长了 3 个字符。
            // 这可能是 payload 被打印到了页面上
            return if (absDiff == payloadLength) {
                listOf(COLOR_IGNORE, COLOR_TEXT) // 反射 -> 灰色
            } else {
                listOf(COLOR_INTERESTING, COLOR_TEXT) // 结构性变化 -> 绿色
            }
        }

        // 4. [兜底逻辑] 状态码判断
        return when (responseCode) {
            // 500, 502, 503: 服务端崩了，可能是盲注导致数据库未处理异常 -> 值得关注 (橙色)
            in 500..599 -> listOf(COLOR_WARNING, COLOR_TEXT)
            400, 404 -> listOf(COLOR_IGNORE, COLOR_TEXT)
            else -> listOf(COLOR_IGNORE, COLOR_TEXT)
        }
    }
}