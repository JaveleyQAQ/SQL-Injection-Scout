package ui.components

import burp.api.montoya.ui.editor.HttpRequestEditor
import burp.api.montoya.ui.editor.HttpResponseEditor
import model.logentry.ModifiedLogEntry
import java.awt.Component
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableRowSorter

class ModifiedLogTable(
    private val modifiedLog: ModifiedLogEntry,
    private val requestView: HttpRequestEditor,
    private val responseView: HttpResponseEditor
) : JTable(modifiedLog) {

    private val tableRowSorter = TableRowSorter(model)

    // 性能优化：静态编译正则，避免在排序循环中重复编译 (非常重要)
    companion object {
        private val DIFF_NUM_REGEX = """[+-]?\s*\d+""".toRegex()
    }

    init {
        autoCreateRowSorter = true
        setupTableProperties()
        setupSorting()
        setupCellRenderer()
        // init 时不一定有数据，这里不需要调 sortByColor
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
        tableRowSorter.model = model
        // 为每一列设置比较器
        for (i in 0 until columnCount) {
            tableRowSorter.setComparator(i) { o1, o2 ->
                compareMixedValues(o1.toString(), o2.toString())
            }
        }
        rowSorter = tableRowSorter
    }

    /**
     * 核心方法：重置排序为默认状态（即 Model 的顺序）
     * 当你在主表切换日志时调用此方法
     */
    fun resetSortToDefault() {
        // 清空所有排序键，JTable 将直接显示 Model 的数据顺序
        // 因为 Model 已经被 sortByColor() 排好序了，所以这里只需要让 View 放弃干预即可
        tableRowSorter.sortKeys = null
    }

    // 性能优化的比较器
    private fun compareMixedValues(s1: String, s2: String): Int {
        val num1 = extractDiffNumber(s1)
        val num2 = extractDiffNumber(s2)

        return when {
            num1 != null && num2 != null -> num1.compareTo(num2)
            num1 != null -> -1 // 带有数字的（Diff）排在前面
            num2 != null -> 1
            else -> s1.compareTo(s2, ignoreCase = true)
        }
    }

    private fun extractDiffNumber(s: String): Int? {
        // 使用预编译的 Regex，性能提升百倍
        return DIFF_NUM_REGEX.find(s)?.value?.replace(" ", "")?.toIntOrNull()
    }

    private fun setupCellRenderer() {
        setDefaultRenderer(Any::class.java, object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
            ): Component {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

                // 性能优化：如果选中，直接返回默认样式，不需要查 Model（避免选中后颜色看不清）
                if (isSelected) return this

                val modelRow = convertRowIndexToModel(row)
                val currentMD5 = modifiedLog.getCurrentMD5()
                val entry = modifiedLog.getModifiedEntry(currentMD5, modelRow)

                // 设置背景色
                background = entry?.color?.get(0) ?: table.background
                foreground = entry?.color?.get(1) ?: table.foreground

                return this
            }
        })
    }

    override fun changeSelection(viewRowIndex: Int, columnIndex: Int, toggle: Boolean, extend: Boolean) {
        // 增加范围检查，防止越界
        if (viewRowIndex in 0 until rowCount) {
            val modelRow = convertRowIndexToModel(viewRowIndex)
            modifiedLog.setCurrentRowIndex(modelRow)

            val currentMD5 = modifiedLog.getCurrentMD5()
            val entry = modifiedLog.getModifiedEntry(currentMD5, modelRow)

            entry?.let {
                // 仅当请求对象不同时才更新 Editor，减少闪烁
                if (requestView.request != it.httpRequestResponse.request()) {
                    requestView.request = it.httpRequestResponse.request()
                    responseView.response = it.httpRequestResponse.response()
                }
                // 搜索定位
                if (it.diffString!!.isNotBlank()) {
                    responseView.setSearchExpression(it.diffString)
                }
            }
        }
        super.changeSelection(viewRowIndex, columnIndex, toggle, extend)
    }
}