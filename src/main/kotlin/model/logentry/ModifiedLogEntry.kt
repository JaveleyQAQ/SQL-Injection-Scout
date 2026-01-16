package model.logentry

import processor.helper.color.ModifiedEntrySortHelper
import processor.helper.color.ModifiedLoggerResponseHelper

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.SwingUtilities
import javax.swing.table.AbstractTableModel
import kotlin.concurrent.Volatile

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
    @Volatile
    private var cachedMD5: String? = null

    private var currentRow: Int = -1  // 用于记录table2中选中的row
    private val entryCompletionCounters: ConcurrentHashMap<String, AtomicInteger> = ConcurrentHashMap()

    fun addExpectedEntriesForMD5(md5: String, count: Int) {
        entryCompletionCounters.computeIfAbsent(md5) { AtomicInteger(count) }
    }

    private fun checkEntryCompletion(md5: String) {
        entryCompletionCounters[md5]?.let { counter ->
            synchronized(counter) {
                val remaining = counter.decrementAndGet()
                if (remaining <= 0) {
                    Thread { onAllEntriesAdded(md5) }.start()
                    entryCompletionCounters.remove(md5)
                }
            }
        }
    }

    /**
     *     所有payload执行完毕后后检查操作
      */
    private fun onAllEntriesAdded(md5: String): Boolean {
        logEntry.setIsChecked(md5, true)
        val logs = logEntry.getEntry(md5) ?: return false

        ModifiedLoggerResponseHelper.processEntries(logs)
        val checkInteresting =  ModifiedLoggerResponseHelper.checkInteresting(logs)

        println("All entries for MD5 $md5 have been added. is checkInteresting ? =$checkInteresting, entries=${logs.modifiedEntries.size}")
        return true
    }

    override fun getRowCount(): Int {
        return cachedMD5?.let { md5 ->
            logEntry.getEntry(md5)?.modifiedEntries?.size ?: 0
        } ?: 0
    }
    override fun getColumnCount(): Int = columnNames.size

    override fun getColumnName(column: Int): String = columnNames[column]

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val md5 = cachedMD5 ?: return ""
        val entry = logEntry.getEntry(md5) ?: return ""
        val entries = entry.modifiedEntries

        return synchronized(entries) { // 确保线程安全
            if (rowIndex < 0 || rowIndex >= rowCount) return ""
            val modifiedEntry = entries[rowIndex]
            when (columnNames[columnIndex]) {
                "#" -> rowIndex  // 0 开始
                "parameter" -> modifiedEntry.parameter
                "payload" -> modifiedEntry.payload
                "diff" -> modifiedEntry.diff
                "status" -> modifiedEntry.status
                "time" -> modifiedEntry.time
                else -> ""
            }
        }
    }

    fun setCurrentEntry(md5: String) {
        if (cachedMD5 != md5) {
            cachedMD5 = md5
            fireTableDataChanged()
        }
    }

    fun setCurrentRowIndex(index: Int) {
        currentRow = index
    }

    fun getModifiedEntry(md5: String?, index: Int): ModifiedLogDataModel? {
        if (md5 == null) return null
        val entries = logEntry.getEntry(md5)?.modifiedEntries?.toList()
        return entries?.getOrNull(index)
    }


    fun addModifiedEntry(md5: String, modifiedEntry: ModifiedLogDataModel, diffString: String?) {
        logEntry.getEntry(md5)?.modifiedEntries?.let { entries ->
            modifiedEntry.diffString = diffString

            synchronized(entries) {
                entries.add(modifiedEntry)
                val newIndex = entries.size - 1

                // 双重验证：确保索引在模型范围内
                if (newIndex >= 0 && newIndex < rowCount) {
                    SwingUtilities.invokeLater {
                        fireTableRowsInserted(newIndex, newIndex)
                    }
                }
            }
            checkEntryCompletion(md5)
        }
    }
    /**
     *  对日志列表进行颜色排序
     */
    fun sortByColor() {
        this.cachedMD5?.let { md5 ->
            logEntry.getEntry(md5)?.let { entry ->
                SwingUtilities.invokeLater {
                    ModifiedEntrySortHelper.sortByColor(entry.modifiedEntries)
                    fireTableDataChanged()
                }
            }
        }
    }

    fun clear() {
        this.cachedMD5 = null
        entryCompletionCounters.clear()
        fireTableDataChanged()
    }

    fun getCurrentMD5(): String? = cachedMD5
    fun getCurrentRow(): Int = currentRow
}
