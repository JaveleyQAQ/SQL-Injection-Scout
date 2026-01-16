package processor.helper.color

import model.config.ColorGroupedData
import model.logentry.ModifiedLogDataModel
import java.awt.Color

object ModifiedEntrySortHelper {

    fun sortByColor(entries: MutableList<ModifiedLogDataModel>) {
        // 分离灰色条
        val (nonGray, gray) = entries.partition { it.color[0] != Color.LIGHT_GRAY }

        // 预处理颜色优先级映射
        val priorityMap = nonGray.associateWith { ColorSortHelper.getColorPriority(it.color[0]) ?: Int.MAX_VALUE }

        // 分组并计算最小优先级
        val groups = nonGray.groupBy { it.parameter }.map { (param, entries) ->
            val minPriority = entries.minOfOrNull { priorityMap[it] ?: Int.MAX_VALUE }
                ?: Int.MAX_VALUE
            ColorGroupedData(param, entries, minPriority)
        }

        // 执行排序并更新原始列表
        entries.run {
            clear()
            addAll(
                groups.sortedWith(compareBy({ it.colorPriority }, { it.parameter.lowercase() }))
                    .flatMap { group ->
                        group.entries.sortedWith(
                            compareBy<ModifiedLogDataModel>({ priorityMap[it] ?: Int.MAX_VALUE })
                                .thenByDescending { ColorSortHelper.parseDiffValue(it.diff) }
                                .thenBy { it.payload }
                        )
                    } + gray.sortedBy { it.parameter }
            )
        }

    }
}