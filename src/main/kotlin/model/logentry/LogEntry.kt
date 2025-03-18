package model.logentry

import burp.api.montoya.MontoyaApi
import burp.api.montoya.http.message.HttpRequestResponse
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
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
    private val entries = CopyOnWriteArrayList<LogEntryModel>()
    private val md5ToEntry = ConcurrentHashMap<String, LogEntryModel>()
    private val nextId = AtomicInteger(0)
    private val columnNames = listOf("#", "Method", "Host", "Path", "Status", "Body Len", "MIME Type", "Flag")

    override fun getRowCount(): Int = entries.size
    override fun getColumnCount(): Int = columnNames.size
    override fun getColumnName(column: Int): String = columnNames[column]

    @Synchronized
    private fun addEntry(
        parametersMD5: String,
        requestResponse: HttpRequestResponse,
        comments: String? = null
    ): Int {
        if (md5ToEntry.containsKey(parametersMD5)) return -1

        val newId = nextId.getAndIncrement()
        val entry = LogEntryModel(
            id = newId,
            requestResponse = requestResponse,
            parametersMD5 = parametersMD5,
            isChecked = false,
            comments = comments
        )

        md5ToEntry[parametersMD5] = entry
        entries.add(entry)

        // 关键，在添加元素后立即获取索引，避免竞争条件导致 index out range
        val currentRowCount = entries.size
        val rowIndex = currentRowCount - 1

        SwingUtilities.invokeLater {
            if (rowIndex in 0 until currentRowCount) {
                fireTableRowsInserted(rowIndex, rowIndex)
            }
        }
        return newId
    }

    fun add(parametersMD5: String, requestResponse: HttpRequestResponse): Int =
        addEntry(parametersMD5, requestResponse)

    fun markRequestWithExcessiveParameters(requestHash: String, requestResponse: HttpRequestResponse): Int =
        addEntry(requestHash, requestResponse, "Excessive Parameters")

    fun markRequestWithSkipRegexURL(requestHash: String, requestResponse: HttpRequestResponse): Int =
        addEntry(requestHash, requestResponse, "Matching Skip Regex URL")

    @Synchronized
    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        if (rowIndex !in 0 until rowCount) return ""
        val entry = entries[rowIndex]
        return when (columnNames[columnIndex]) {
            "#" -> entry.id
            "Method" -> entry.method
            "Host" -> entry.host
            "Path" -> entry.path
            "Status" -> entry.status
            "Body Len" -> entry.bodyLength
            "MIME Type" -> entry.mimeType
            "Flag" -> getFlagDisplay(entry)
            else -> ""
        }
    }

    private fun getFlagDisplay(entry: LogEntryModel): String = when {
        entry.hasVulnerability && entry.isChecked -> "\uD83D\uDD25"
        entry.hasVulnerability -> "\uD83D\uDD25"
        entry.interesting -> "✓"
        entry.isChecked -> "✗"
        entry.comments == "Excessive Parameters" -> "Max Params"
        entry.comments == "Matching Skip Regex URL" -> "Skip URL"
        else -> "Scanning"
    }

    @Synchronized
    fun setVulnerability(parametersMD5: String, hasVulnerability: Boolean) {
        md5ToEntry[parametersMD5]?.let { entry ->
            entry.hasVulnerability = hasVulnerability
            val rowIndex = entries.indexOf(entry)
            if (rowIndex != -1) {
                fireTableCellUpdated(rowIndex, columnNames.indexOf("Flag"))
            }
        }
    }

    fun setIsChecked(parametersMD5: String, isChecked: Boolean) {
        md5ToEntry[parametersMD5]?.let { entry ->
            entry.isChecked = isChecked
            val rowIndex = entries.indexOf(entry)
            SwingUtilities.invokeLater {
                fireTableCellUpdated(rowIndex, columnNames.indexOf("Flag"))
            }
        }
    }

    fun getEntry(parametersMD5: String): LogEntryModel? = md5ToEntry[parametersMD5]

    fun clear() {
        entries.clear()
        md5ToEntry.clear()
        nextId.set(0)
        fireTableDataChanged()
    }
    fun getEntryMD5ByIndex(index: Int): String? = entries.getOrNull(index)?.parametersMD5

}
//    fun getLogs(): ConcurrentHashMap<String, LogEntryModel> {
//        return logs
//    }
//
//    override fun getRowCount(): Int = logs.size
//    override fun getColumnCount(): Int = columnNames.size
//    override fun getColumnName(column: Int): String = columnNames[column]
//
//    fun add(parametersMD5: String, requestResponse: HttpRequestResponse): Int {
//        val existing  = logs.
//
//        val newId = nextId.getAndIncrement() // 安全生成唯一 id
//        val entry = LogEntryModel(
//            id = newId,
//            requestResponse = requestResponse,
//            parametersMD5 = parametersMD5,
//            isChecked = false
//        )
//        logs[parametersMD5] = entry
//        idToEntry[newId] = entry // 同步更新 idToEntry
//        idToMD5[newId] = parametersMD5
//
//        SwingUtilities.invokeLater {
//            val rowIndex = rowCount - 1
//            fireTableRowsInserted(rowIndex, rowIndex)
//        }
//        return newId
//    }
//
//
//    /**
//     * 对超出参数个数范围的请求进行单独标记
//     */
//    @Synchronized
//    fun markRequestWithExcessiveParameters(requestHash: String, requestResponse: HttpRequestResponse) {
//        requestExist(requestHash)?:return
//        val newId = nextId.getAndIncrement()
//        val entry = LogEntryModel(
//            id = newId,
//            requestResponse = requestResponse,
//            parametersMD5 = requestHash,
//            isChecked = false,
//            comments = "Excessive Parameters"
//        )
//        logs[requestHash] = entry
//        idToEntry[newId] = entry
//        idToMD5[newId] = requestHash
//        SwingUtilities.invokeLater {
//            val rowIndex = rowCount - 1  // 使用当前行数减一
//            fireTableRowsInserted(rowIndex, rowIndex)
//        }
//    }
//
//    @Synchronized
//    fun markRequestWithSkipRegexURL(requestHash: String, requestResponse: HttpRequestResponse) {
//        requestExist(requestHash)?:return
//        val newId = nextId.getAndIncrement()
//        val entry = LogEntryModel(
//            id = newId,
//            requestResponse = requestResponse,
//            parametersMD5 = requestHash,
//            isChecked = false,
//            comments = "Matching Skip Regex URL"
//        )
//        logs[requestHash] = entry
//        idToEntry[newId] = entry
//        idToMD5[newId] = requestHash
//        SwingUtilities.invokeLater {
//            val rowIndex = rowCount - 1  // 使用当前行数减一
//            fireTableRowsInserted(rowIndex, rowIndex)
//        }
//    }
//
//    private fun requestExist(requestHash: String): Boolean{
//        return logs.containsKey(requestHash) ?:false
//    }
//
//    @Synchronized
//    fun getEntry(parametersMD5: String?): LogEntryModel? = logs[parametersMD5]
//
//    @Synchronized
//    fun setVulnerability(parametersMD5: String, hasVulnerability: Boolean) {
//        logs[parametersMD5]?.let { entry ->
//            entry.hasVulnerability = hasVulnerability
//            fireTableCellUpdated(entry.id, columnNames.indexOf("Flag"))
//        }
//    }
//
//    fun setIsChecked(parametersMD5: String, isChecked: Boolean) {
//        logs[parametersMD5]?.let { entry ->
//            entry.isChecked = isChecked
//            val columnIndex = columnNames.indexOf("Flag")
//            SwingUtilities.invokeLater {
//                fireTableCellUpdated(entry.id, columnIndex)
//            }
//        }
//    }
//
//    @Synchronized
//    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
//        val entry = idToEntry[rowIndex] ?: return ""
//        return when (columnNames[columnIndex]) {
//            "#" -> entry.id
//            "Method" -> entry.method
//            "Host" -> entry.host
//            "Path" -> entry.path
////            "Title" -> entry.title
//            "Status" -> entry.status
//            "Body Len" -> entry.bodyLength
//            "MIME Type" -> entry.mimeType
//            "Flag" -> when {
//                entry.hasVulnerability && entry.isChecked -> "\uD83D\uDD25"
//                entry.hasVulnerability -> "\uD83D\uDD25"
//                entry.interesting -> "✓"
//                entry.isChecked -> "✗"
//                entry.comments.equals("Excessive Parameters") -> "Max Params"
//                entry.comments.equals("Matching Skip Regex URL") -> "Skip URL"
//                else -> "Scanning"
//            }
//
//
//            else -> ""
//        }
//    }
//
//
//    fun setInteresting(md5: String, value: Boolean) {
//        logs[md5]?.interesting = value
//    }
//
//
//    fun clear() {
//        logs.clear()
//        idToMD5.clear()
//        idToEntry.clear()
//        nextId.set(0)
//        fireTableDataChanged()
//    }
//    fun getEntryMD5ByIndex(index: Int): String? = idToMD5[index]
//}