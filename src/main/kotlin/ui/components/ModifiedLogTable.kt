import burp.api.montoya.ui.editor.HttpRequestEditor
import burp.api.montoya.ui.editor.HttpResponseEditor
import model.logentry.ModifiedLogEntry
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableRowSorter
import java.awt.Component

class ModifiedLogTable(
    private val modifiedLog: ModifiedLogEntry,
    private val requestView: HttpRequestEditor,
    private val responseView: HttpResponseEditor
) : JTable(modifiedLog) {

    private val tableRowSorter = TableRowSorter(model)

    init {
        autoCreateRowSorter = true
        setupTableProperties()
        setupSorting()
        setupCellRenderer()
        modifiedLog.sortByColor()
    }

    private fun setupTableProperties() {
        autoResizeMode = AUTO_RESIZE_ALL_COLUMNS
        selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        columnModel.apply {
            getColumn(0).preferredWidth = 50   // #
            getColumn(1).preferredWidth = 100  // Parameter
            getColumn(2).preferredWidth = 170  // Payload
            getColumn(3).preferredWidth = 100  // Diff
            getColumn(4).preferredWidth = 70   // Status
            getColumn(5).preferredWidth = 60   // Time
        }
    }

    private fun setupSorting() {
        tableRowSorter.model = model // 使用成员变量
        listOf(0,1,2,3,4).forEach { columnIndex ->
            tableRowSorter.setComparator(columnIndex) { o1, o2 ->
                compareMixedValues(o1.toString(), o2.toString())
            }
        }
        rowSorter = tableRowSorter
    }

    fun resetSorter() {
        if (model.rowCount == 0) return // 避免空数据操作

        tableRowSorter.model?.let { sorterModel ->
            tableRowSorter.sortKeys = listOf()
            tableRowSorter.model = sorterModel // 重新绑定模型
            tableRowSorter.allRowsChanged()
            tableRowSorter.sort()
        }
    }

    private fun compareMixedValues(s1: String, s2: String): Int {
        val num1 = extractDiffNumber(s1)
        val num2 = extractDiffNumber(s2)

        return when {
            num1 != null && num2 != null -> num1.compareTo(num2)
            num1 != null -> -1 // 数字优先级高于文本
            num2 != null -> 1
            else -> s1.compareTo(s2, ignoreCase = true) // 纯文本按字母排序
        }
    }
    private fun extractDiffNumber(s: String): Int? {
        // +123、-456
        val pattern = """[+-]? \d+""".toRegex()
        return pattern.find(s)?.value?.toIntOrNull()
    }

    private fun setupCellRenderer() {
        setDefaultRenderer(Any::class.java, object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable,
                value: Any?,
                isSelected: Boolean,
                hasFocus: Boolean,
                row: Int,
                column: Int
            ): Component {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                val modelRow = convertRowIndexToModel(row)
                val currentMD5 = modifiedLog.getCurrentMD5()
                val entry = modifiedLog.getModifiedEntry(currentMD5, modelRow)

                background = when {
                    isSelected -> selectionBackground
                    else -> entry?.color?.get(0) ?: table.background
                }

                foreground = when {
                    isSelected -> selectionForeground
                    else -> entry?.color?.get(1) ?: table.foreground
                }

                return this
            }
        })
    }

    override fun changeSelection(
        viewRowIndex: Int,
        columnIndex: Int,
        toggle: Boolean,
        extend: Boolean
    ) {
        val modelRow = convertRowIndexToModel(viewRowIndex)
        modifiedLog.setCurrentRowIndex(modelRow)

        val currentMD5 = modifiedLog.getCurrentMD5()
        val entry = modifiedLog.getModifiedEntry(currentMD5, modelRow)

        entry?.let {
            requestView.request = it.httpRequestResponse.request()
            responseView.response = it.httpRequestResponse.response()
            responseView.setSearchExpression(it.diffString)
        }
        super.changeSelection(viewRowIndex, columnIndex, toggle, extend)
    }
}