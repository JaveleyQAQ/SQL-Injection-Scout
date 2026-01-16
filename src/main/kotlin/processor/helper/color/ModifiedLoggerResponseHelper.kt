package processor.helper.color

import model.logentry.LogEntryModel
import model.logentry.ModifiedLogDataModel
import java.awt.Color

object ModifiedLoggerResponseHelper {

    fun processEntries(logs: LogEntryModel) {
        // 计算所有 diff 的重复次
        val diffCountMap = calculateDiffCounts(logs.modifiedEntries)

        logs.modifiedEntries.forEach { entry ->

            applySpecialRules(entry, logs.modifiedEntries)
            if (entry.color[0] == null) {
                val diffCount = diffCountMap[entry.diff] ?: 0
                entry.color = ColorManager.determineColor(
                    entry.diff,
                    entry.payload.length,
                    entry.status.toInt(),
                    diffCount
                )
            }
        }

        checkInteresting(logs)
    }

    /**
     * 辅助：一次性计算 diff 重复次数
     */
    private fun calculateDiffCounts(entries: List<ModifiedLogDataModel>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        entries.forEach { map[it.diff] = (map[it.diff] ?: 0) + 1 }
        return map
    }

    /**
     * 应用特殊规则 (Quote / Null)
     */
    private fun applySpecialRules(currentEntry: ModifiedLogDataModel, allEntries: List<ModifiedLogDataModel>) {
        //  NullPayload导致内容减少 灰色
        if (currentEntry.payload.equals("null", ignoreCase = true) || currentEntry.payload == "{}") {
            if (currentEntry.diff.startsWith("-")) {
                currentEntry.updateColor(Color.LIGHT_GRAY)
                return
            }
        }

        // 单双引号配对检测
        val p = currentEntry.payload
        if (p == "'''" || p == "''''") {
            val partnerPayload = if (p == "'''") "''''" else "'''"
            val partner = allEntries.find {
                it.parameter == currentEntry.parameter && it.payload == partnerPayload
            }

            if (partner != null) {
                val isSingleDiff = (p == "'''" && currentEntry.diff != "same")
                val isDoubleSame = (partnerPayload == "''''" && partner.diff == "same")

                if (isSingleDiff && isDoubleSame) {
                    currentEntry.updateColor(Color.YELLOW)
                    partner.updateColor(Color.YELLOW)
                }
            }
        }
    }

    fun checkInteresting(logs: LogEntryModel): Boolean {
        // 只要存在漏洞 或 颜色不是灰，就是interesting
        val isInteresting = logs.modifiedEntries.any {
            it.vulnerability || (it.color.getOrNull(0) != null && it.color[0] != Color.LIGHT_GRAY)
        }
        logs.interesting = isInteresting
        return isInteresting
    }

    private fun ModifiedLogDataModel.updateColor(c: Color) {
        this.color = listOf(c, Color.BLACK)
    }
}