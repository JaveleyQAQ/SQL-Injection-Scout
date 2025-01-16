package model.logentry

import java.awt.Color
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.table.AbstractTableModel
import kotlin.math.log

/*
 * 记录已被修改的日志模型
 *
 * @author JavelyQAQ
 * @date 2024/10/16
 * @since 1.0.0
 * @see LoggerEntry
 */

class ModifiedLogEntry(private val logEntry: LogEntry) : AbstractTableModel() {
    private val columnNames = listOf("#", "parameter", "payload", "diff", "status", "time")
    private var currentMD5: String? = null  //用于记录table1中选中的row
    private var currentRow: Int = -1  // 用于记录table2中选中的row
    private val entryCompletionCounters: ConcurrentHashMap<String, AtomicInteger> = ConcurrentHashMap()
    private val colorManager = ColorManager()

    fun addExpectedEntriesForMD5(md5: String, count: Int) {
        entryCompletionCounters.computeIfAbsent(md5) { AtomicInteger(count) }
    }

    private fun checkEntryCompletion(md5: String) {
        entryCompletionCounters[md5]?.decrementAndGet()?.let { remaining ->
            if (remaining <= 0) {
                onAllEntriesAdded(md5)
                entryCompletionCounters.remove(md5) // 清理不再需要的计数器
            }
        }
    }

    private fun checkDiffString(md5: String, logs: LogEntryModel?) {
        val diffCountMap = mutableMapOf<String, Int>()
        val entriesToRecolor = mutableListOf<Int>()
        var hasInteresting = false

        logs?.modifiedEntries?.forEach { entry ->
            val currentCount = diffCountMap.getOrDefault(entry.diff, 0)
            diffCountMap[entry.diff] = currentCount + 1
        }

        logs?.modifiedEntries?.forEachIndexed { index, entry ->
            val responseCode = entry.status.toInt()
            val diffCount = diffCountMap[entry.diff] ?: 0
            if (entry.color[0] == null){
                entry.color = colorManager.determineColor(entry.diff, entry.payload.length, responseCode, diffCount)
            }

            if ( entry.color[0] == Color.GREEN) {
                hasInteresting = true
                logs.interesting = true
            } else if ( entry.color[0] == Color.LIGHT_GRAY) {
                entriesToRecolor.add(index)
            }
        }

        if (!hasInteresting) {
            logs?.interesting = false
        }
    }


    // 完成后检查操作
    private fun onAllEntriesAdded(md5: String): Boolean {
        logEntry.setIsChecked(md5, true)
        val logs = logEntry.getLogs()[md5]
        checkDiffString(md5, logs)

        val allDiffsAreSame = logs?.modifiedEntries?.all { it.diff == "same" }
        val hasVulnerability = logs?.modifiedEntries?.any { it.vulnerability }

        // 修改判断逻辑：检查第一个颜色值是否都是LIGHT_GRAY
        val allColorAreGray = logs?.modifiedEntries?.let { entries ->
            entries.isNotEmpty() && entries.all { entry ->
                entry.color[0] == Color.LIGHT_GRAY
            }
        } ?: false

        val hasAnyColor = logs?.modifiedEntries?.any { it.color[0] != null } ?: false

        // 对扫描完成的任务做 有趣 🤔 分析
        logs?.interesting = when {
            hasVulnerability == true -> true
            allDiffsAreSame == true && logs.isChecked -> false
            allColorAreGray -> false  // 如果所有颜色都是灰色，则不有趣
            else -> true
        }

        println("All entries for MD5 $md5 have been added. allColorAreGray=$allColorAreGray, entries=${logs?.modifiedEntries?.size}")
        sortByColor()
        return true
    }

    override fun getRowCount(): Int =
        currentMD5?.let { logEntry.getEntry(it)?.modifiedEntries?.size } ?: 0

    override fun getColumnCount(): Int = columnNames.size

    override fun getColumnName(column: Int): String = columnNames[column]

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val entry = currentMD5?.let { logEntry.getEntry(it) } ?: return ""
        if (rowIndex >= entry.modifiedEntries.size) return ""
        val modifiedEntry = entry.modifiedEntries[rowIndex]
        return when (columnNames[columnIndex]) {
            "#" -> rowIndex + 1
            "parameter" -> modifiedEntry.parameter
            "payload" -> modifiedEntry.payload
            "diff" -> modifiedEntry.diff
            "status" -> modifiedEntry.status
            "time" -> modifiedEntry.time
            else -> ""
        }
    }


    fun setCurrentEntry(md5: String) {
        currentMD5 = md5
        fireTableDataChanged()
    }

    fun setCurrentRowIndex(index: Int) {
        currentRow = index
    }


    fun getModifiedEntry(md5: String?, index: Int): ModifiedLogDataModel? {
        if (md5 == null) return null
        val entries = logEntry.getEntry(md5)?.modifiedEntries?.toList()
        return entries?.getOrNull(index)
    }
    @Synchronized
    fun addModifiedEntry(md5: String, modifiedEntry: ModifiedLogDataModel, diffString: String?) {
        logEntry.getEntry(md5)?.modifiedEntries?.let { entries ->
            modifiedEntry.diffString = diffString
            entries.add(modifiedEntry)
            if (md5 == currentMD5) {
                sortByColor()
                fireTableDataChanged()
            }
            checkEntryCompletion(md5)
        }
    }


    // 根据颜色进行排序
    fun sortByColor() {
        currentMD5?.let { md5 ->
            logEntry.getEntry(md5)?.let { entry ->
                // 首先将条目分成两组：灰色和非灰色
                val (grayEntries, nonGrayEntries) = entry.modifiedEntries.partition { 
                    it.color[0] == Color.LIGHT_GRAY 
                }
                
                // 对非灰色条目进行排序，简化次级排序逻辑
                val sortedNonGray = nonGrayEntries.sortedWith(
                    compareBy<ModifiedLogDataModel> { modifiedEntry ->
                        // 第一级排序：按颜色优先级（主要排序）
                        when (modifiedEntry.color[0]) {
                            Color.RED -> 0    // SQL注入等高危漏洞最优先
                            null -> 1         // 未分类结果次之
                            Color.GREEN -> 2  // 有趣的结果第三
                            else -> 3         // 其他颜色
                        }
                    }.thenBy { modifiedEntry ->
                        // 第二级排序：按照parameter名称
                        modifiedEntry.parameter
                    }.thenBy { modifiedEntry ->
                        // 第三级排序：简化diff值处理
                        when {
                            modifiedEntry.diff == "Error" -> -100000  // Error放最前面
                            modifiedEntry.diff == "same" -> 100000    // same放最后面
                            else -> {
                                val num = modifiedEntry.diff.replace("+", "")
                                    .replace("-", "")
                                    .toIntOrNull() ?: 0
                                -num  // 大的数值排在前面
                            }
                        }
                    }
                )

                // 将灰色条目直接添加到末尾
                entry.modifiedEntries.clear()
                entry.modifiedEntries.addAll(sortedNonGray)
                entry.modifiedEntries.addAll(grayEntries)
                
                fireTableDataChanged()
            }
        }
    }


    fun clear() {
        currentMD5 = null
        entryCompletionCounters.clear()
        fireTableDataChanged()
    }

    fun getCurrentMD5(): String? = currentMD5
    fun getCurrentRow(): Int = currentRow
}