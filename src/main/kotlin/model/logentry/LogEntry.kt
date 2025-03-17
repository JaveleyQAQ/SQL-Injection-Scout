package model.logentry

import burp.api.montoya.MontoyaApi
import burp.api.montoya.http.message.HttpRequestResponse
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.SwingUtilities
import javax.swing.table.AbstractTableModel

/*
 * 日志表格
 * @author JaveleyQAQ
 * @since 2024/10/16
 * @version 1.0
 * @see LogEntry
 */

class LogEntry(val api: MontoyaApi) : AbstractTableModel() {
    private val logs: ConcurrentHashMap<String, LogEntryModel> = ConcurrentHashMap()
    private val idToMD5 = ConcurrentHashMap<Int, String>()
    private val idToEntry = ConcurrentHashMap<Int, LogEntryModel>()
    private val nextId = AtomicInteger(0)
    private val columnNames =
        listOf("#", "Method", "Host", "Path", "Status", "Body Len", "MIME Type", "Flag")


    fun getLogs(): ConcurrentHashMap<String, LogEntryModel> {
        return logs
    }

    override fun getRowCount(): Int = logs.size
    override fun getColumnCount(): Int = columnNames.size
    override fun getColumnName(column: Int): String = columnNames[column]

    @Synchronized
    fun add(parametersMD5: String, requestResponse: HttpRequestResponse): Int {
        if (logs.containsKey(parametersMD5)) return -1

        val newId = nextId.getAndIncrement() // 安全生成唯一 id
        val entry = LogEntryModel(
            id = newId,
            requestResponse = requestResponse,
            parametersMD5 = parametersMD5,
            isChecked = false
        )
        logs[parametersMD5] = entry
        idToEntry[newId] = entry // 同步更新 idToEntry
        idToMD5[newId] = parametersMD5

        SwingUtilities.invokeLater { fireTableRowsInserted(newId, newId) }
        return newId
    }


    /**
     * 对超出参数个数范围的请求进行单独标记
     */
    @Synchronized
    fun markRequestWithExcessiveParameters(requestHash: String, requestResponse: HttpRequestResponse) {
        if (logs.containsKey(requestHash)) return

        val newId = nextId.getAndIncrement()
        val entry = LogEntryModel(
            id = newId,
            requestResponse = requestResponse,
            parametersMD5 = requestHash,
            isChecked = false,
            comments = "Excessive Parameters"
        )
        logs[requestHash] = entry
        idToEntry[newId] = entry
        idToMD5[newId] = requestHash


        SwingUtilities.invokeLater { fireTableRowsInserted(newId, newId) }
    }

    @Synchronized
    fun getEntry(parametersMD5: String?): LogEntryModel? = logs[parametersMD5]

    @Synchronized
    fun setVulnerability(parametersMD5: String, hasVulnerability: Boolean) {
        logs[parametersMD5]?.let { entry ->
            entry.hasVulnerability = hasVulnerability
            fireTableCellUpdated(entry.id, columnNames.indexOf("Flag"))
        }
    }

    fun setIsChecked(parametersMD5: String, isChecked: Boolean) {
        logs[parametersMD5]?.let { entry ->
            entry.isChecked = isChecked
            val columnIndex = columnNames.indexOf("Flag")
            SwingUtilities.invokeLater {
                fireTableCellUpdated(entry.id, columnIndex)
            }
        }
    }

    @Synchronized
    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val entry = idToEntry[rowIndex] ?: return ""
        return when (columnNames[columnIndex]) {
            "#" -> entry.id
            "Method" -> entry.method
            "Host" -> entry.host
            "Path" -> entry.path
//            "Title" -> entry.title
            "Status" -> entry.status
            "Body Len" -> entry.bodyLength
            "MIME Type" -> entry.mimeType
            "Flag" -> when {
                entry.hasVulnerability && entry.isChecked -> "\uD83D\uDD25"
                entry.hasVulnerability -> "\uD83D\uDD25"
                entry.interesting -> "✓"
                entry.isChecked -> "✗"
                entry.comments.equals("Excessive Parameters") -> "Max param"
                else -> "Scanning"
            }


            else -> ""
        }
    }


    fun setInteresting(md5: String, value: Boolean) {
        logs[md5]?.interesting = value
    }


    fun clear() {
        logs.clear()
        idToMD5.clear()
        idToEntry.clear()
        nextId.set(0)
        fireTableDataChanged()
    }
    fun getEntryMD5ByIndex(index: Int): String? = idToMD5[index]
}