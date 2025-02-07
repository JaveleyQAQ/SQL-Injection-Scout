package model.logentry

import burp.api.montoya.MontoyaApi
import burp.api.montoya.http.message.HttpRequestResponse
import java.util.concurrent.ConcurrentHashMap
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

        //检测是否重复
        if (!logs.containsKey(parametersMD5)) {
            val index = logs.size
            logs[parametersMD5] = LogEntryModel(
                id = index,
                requestResponse = requestResponse,
                parametersMD5 = parametersMD5,
                isChecked = false
            )
            fireTableRowsInserted(index, index)
            return index
        }
        return -1
    }

    /**
     * 对超出参数个数范围的请求进行单独标记
     */
    @Synchronized
    fun markRequestWithExcessiveParameters(requestHash: String, requestResponse: HttpRequestResponse) {
        // 检测是否重复
        if (!logs.containsKey(requestHash)) {
            val logEntryIndex = logs.size
            logs[requestHash] = LogEntryModel(
                id = logEntryIndex,
                requestResponse = requestResponse,
                parametersMD5 = requestHash,
                isChecked = false,
                comments = "Excessive Parameters"
            )
            fireTableRowsInserted(logEntryIndex, logEntryIndex)
        }
    }

    fun getEntry(parametersMD5: String): LogEntryModel? = logs[parametersMD5]

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
            fireTableRowsDeleted(entry.id, columnNames.indexOf("Flag"))
        }
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val entry = logs.values.find { it.id == rowIndex } ?: return ""
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
                entry.interesting -> "Interesting"
                entry.isChecked -> "Boring"
                entry.comments.equals("Excessive Parameters") -> "ExcessParams"
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
        fireTableDataChanged()
    }

    fun getEntryMD5ByIndex(index: Int): String? {
        return logs.entries.find { it.value.id == index }?.key
    }
}