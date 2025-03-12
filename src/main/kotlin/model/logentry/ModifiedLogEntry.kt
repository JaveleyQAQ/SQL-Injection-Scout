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
    private var cachedLogEntries: LogEntryModel? = null
    //@Volatile
    @Volatile
    private var cachedMD5: String? = null

    private var currentRow: Int = -1  // 用于记录table2中选中的row
    private val entryCompletionCounters: ConcurrentHashMap<String, AtomicInteger> = ConcurrentHashMap()

    fun addExpectedEntriesForMD5(md5: String, count: Int) {
        entryCompletionCounters.computeIfAbsent(md5) { AtomicInteger(count) }
    }

    private fun checkEntryCompletion(md5: String) {
        entryCompletionCounters[md5]?.decrementAndGet()?.let { remaining ->
            if (remaining <= 0) {
                Thread {
                    onAllEntriesAdded(md5) // 如果onAllEntriesAdded有耗时操作，也移到后台
                }.start()
                entryCompletionCounters.remove(md5)
            }
        }
    }

    /**
     *     所有payload执行完毕后后检查操作
      */
    private fun onAllEntriesAdded(md5: String): Boolean {
        logEntry.setIsChecked(md5, true)
        val logs = logEntry.getLogs()[md5] ?: return false

        ModifiedLoggerResponseHelper.processEntries(logs)
        val checkInteresting =  ModifiedLoggerResponseHelper.checkInteresting(logs)

        println("All entries for MD5 $md5 have been added. is checkInteresting ? =$checkInteresting, entries=${logs.modifiedEntries.size}")
        return true
    }

    override fun getRowCount(): Int =
        this.cachedMD5?.let{ logEntry.getEntry(it)?.modifiedEntries?.size } ?: 0

    override fun getColumnCount(): Int = columnNames.size

    override fun getColumnName(column: Int): String = columnNames[column]

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {

        this.cachedLogEntries = logEntry.getEntry(this.cachedMD5)
        val entries = this.cachedLogEntries ?:return ""
        if (rowIndex >= entries.modifiedEntries.size) return ""
        val modifiedEntry = entries.modifiedEntries[rowIndex]
        return when (columnNames[columnIndex]) {
            "#" -> rowIndex
            "parameter" -> modifiedEntry.parameter
            "payload" -> modifiedEntry.payload
            "diff" -> modifiedEntry.diff
            "status" -> modifiedEntry.status
            "time" -> modifiedEntry.time
            else -> " "
        }
    }

    fun setCurrentEntry(md5: String) {
        this.cachedMD5 = md5
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

    fun addModifiedEntry(md5: String, modifiedEntry: ModifiedLogDataModel, diffString: String?) {
        var index = -1
        logEntry.getEntry(md5)?.modifiedEntries?.let { entries ->
            modifiedEntry.diffString = diffString
            entries.add(modifiedEntry)
            index = entries.size
            checkEntryCompletion(md5)
        }
        SwingUtilities.invokeLater { fireTableRowsInserted(index, index) }
    }





    /**
     *  对日志列表进行颜色排序
     */
    fun  sortByColor(){
        this.cachedMD5?.let { md5 ->
            logEntry.getEntry(md5)?.let { entry ->
                ModifiedEntrySortHelper.sortByColor(entry.modifiedEntries)
            }
        }
        SwingUtilities.invokeLater { fireTableDataChanged() }
    }

    fun clear() {
        this.cachedMD5 = null
        entryCompletionCounters.clear()
        fireTableDataChanged()
    }

    fun getCurrentMD5(): String? = cachedMD5
    fun getCurrentRow(): Int = currentRow
}
