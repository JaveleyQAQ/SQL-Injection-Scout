package processor.helper.color

import processor.helper.color.ColorManager
import model.logentry.LogEntryModel
import model.logentry.ModifiedLogDataModel
import java.awt.Color

/**
 *  1. 单独标记单双引号response
 *  2. null 的结果如果少于原始请求则默认为无意义
 *  3. 统计diff重复次数，大于6次则标记灰色，其他单独使用ColorManager处理
 *  3. 根据以上的逻辑，只要存在漏洞或非灰色颜色，即为 interesting
 */


object ModifiedLoggerResponseHelper {

    fun processEntries(logs: LogEntryModel) {

        markQuotePayloadColor(logs.modifiedEntries)
        markNullPayloadColor(logs.modifiedEntries)
        markDiffColors(logs)

    }

    /**
     * 单双引号单独标记差异
     */
    private fun markQuotePayloadColor(entries: MutableList<ModifiedLogDataModel>) {
        // 按 parameter 分组
        val groupedByParam = mutableMapOf<String, MutableList<ModifiedLogDataModel>>()
        entries.forEach { entry ->
            groupedByParam.getOrPut(entry.parameter) { mutableListOf() }.add(entry)
        }

        groupedByParam.forEach { (param, group) ->
            // 检查是否有 payload 为 "'''" 或 "''''"
            val hasTarget = group.any { it.payload in listOf("'''", "''''") }
            if (hasTarget) {
                val single = group.find { it.payload == "'''" }
                val double = group.find { it.payload == "''''" }
                if (single?.diff != "same" && double?.diff == "same") {
                    val res1 = single?.httpRequestResponse?.response()
                    val res2 = double.httpRequestResponse.response()
                    if (res1 != res2) {
                        single?.updateColor(Color.YELLOW)
                        double.updateColor(Color.YELLOW)
                    }
                }
            }
            // 检查是否有 payload 为 "'''" 或 "''''"
            val hasTarget2 = group.any { it.payload in listOf("#xx}", "#{xx}") }
            if (hasTarget2) {
                val single = group.find { it.payload == "#xx}" }
                val double = group.find { it.payload == "#{xx}" }
                if (single?.diff != "same" && double?.diff == "same") {
                    val res1 = single?.httpRequestResponse?.response()
                    val res2 = double.httpRequestResponse.response()
                    if (res1 != res2) {
                        single?.updateColor(Color.YELLOW)
                        double.updateColor(Color.YELLOW)
                    }
                }
            }

        }
    }

    /**
     * 对 null 的response结果进行标记
     */
    private fun markNullPayloadColor(entries: MutableList<ModifiedLogDataModel>) {
        entries.parallelStream().forEach { entry ->  //
            if (entry.payload.matches(Regex("""(null|\{\})""", RegexOption.IGNORE_CASE)) && entry.diff.contains("-")) {
                entry.updateColor(Color.LIGHT_GRAY)
            }
        }
    }


    @Synchronized
    fun markDiffColors(logs: LogEntryModel) {
        val diffCountMap = mutableMapOf<String, Int>()
        var hasInteresting = false


        logs.modifiedEntries.forEach { entry ->

            diffCountMap[entry.diff] = (diffCountMap[entry.diff] ?: 0) + 1

        }
        logs.modifiedEntries.forEach { entry ->
            val responseCode = entry.status.toInt()
            val diffCount = diffCountMap[entry.diff] ?: 0

            if (entry.color[0] == null && (entry.diff.contains("+") || entry.diff.contains("-"))) {

                // diff 次数重复超过6次则认为无趣的
                if (diffCount > 6) {
                    entry.color = mutableListOf(Color.LIGHT_GRAY, null)
                } else {  //否则标记绿色
                    entry.color = mutableListOf(Color.GREEN, Color.BLACK)
                }
            } else { //少于6词单独计算,且无颜色的
                if (entry.color[0]==null) {
                    entry.color = ColorManager.determineColor(entry.diff, entry.payload.length, responseCode, diffCount)
                }

            }
            hasInteresting = hasInteresting || (entry.color[0] == Color.GREEN)
        }

    }

    fun checkInteresting(logs: LogEntryModel): Boolean {
        // 检查是否存在至少一个漏洞为 true
        val hasVulnerability = logs.modifiedEntries.any { it.vulnerability }

        // 检查是否存在至少一个颜色不为浅灰
        val hasNonGrayColor = logs.modifiedEntries.any {
            it.color.getOrNull(0) != Color.LIGHT_GRAY
        }

        // 最终判断：只要存在漏洞或非灰色颜色，即为 interesting
        val isInteresting = hasVulnerability || hasNonGrayColor
        logs.interesting = isInteresting
        return isInteresting
    }

    private fun ModifiedLogDataModel.updateColor(color: Color) {
        this.color = listOf(color, Color.BLACK)
    }


}