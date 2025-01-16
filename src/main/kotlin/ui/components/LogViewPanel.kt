package ui.components

import burp.api.montoya.MontoyaApi
import burp.api.montoya.ui.editor.EditorOptions
import config.DataPersistence
import controller.HttpInterceptor
import model.logentry.LogEntry
import model.logentry.ModifiedLogEntry
import java.awt.Color
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer

class LogViewPanel(
    private val api: MontoyaApi,
    private val logs: LogEntry,
    private val modifiedLog: ModifiedLogEntry,
    private val httpInterceptor: HttpInterceptor,
    private val dataPersistence: DataPersistence
) {
    private val userInterface = api.userInterface()
    private var currentMD5: String? = null

    fun buildUI(): Component {
        val dashBoardPanel = JTabbedPane()
        val logViewSplitPanel = JSplitPane(JSplitPane.VERTICAL_SPLIT)
        val requestResponseTabs = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)  //用于展示底部 request 和 response 的数据面板
        // 创建请求响应编辑器
        val requestView = userInterface.createHttpRequestEditor(EditorOptions.READ_ONLY)
        val responseView = userInterface.createHttpResponseEditor(EditorOptions.READ_ONLY)
        requestResponseTabs.leftComponent = requestView.uiComponent()
        requestResponseTabs.rightComponent = responseView.uiComponent()
        logViewSplitPanel.rightComponent = requestResponseTabs //日志展示在底部

        // 创建右键菜单
        val popupMenu = JPopupMenu()
        val clearLogsMenuItem = JMenuItem("Clear History")
        popupMenu.add(clearLogsMenuItem)

        // 主日志表格
        val logTable = object : JTable(logs) {
            override fun changeSelection(rowIndex: Int, columnIndex: Int, toggle: Boolean, extend: Boolean) {

                currentMD5 = logs.getEntryMD5ByIndex(rowIndex).toString()
                modifiedLog.setCurrentEntry(currentMD5!!)
                responseView.setSearchExpression("")
                if (currentMD5 != null) {
                    val requestResponse = logs.getEntry(currentMD5!!)?.requestResponse
                    if (requestResponse != null) {
                        requestView.request = requestResponse.request()
                        responseView.response = requestResponse.response()
                    }
                }

                super.changeSelection(rowIndex, columnIndex, toggle, extend)
            }
        }

        // 设置表格属性
        logTable.apply {
            autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS  // 自动调整列大小
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            columnModel.apply {
                getColumn(0).preferredWidth = 50  // #
                getColumn(1).preferredWidth = 70  // Method
                getColumn(2).preferredWidth = 150 // Host
                getColumn(3).preferredWidth = 400 // Path
                getColumn(4).preferredWidth = 50 // Status
                getColumn(5).preferredWidth = 70  // Body Length
                getColumn(6).preferredWidth = 80 //  MIME Type
                getColumn(7).preferredWidth = 70 // Flag
//                getColumn(8).preferredWidth = 70  // Vulnerability
            }
        }

        // 修改日志表格
        val modifiedLogTable = object : JTable(modifiedLog) {
            override fun changeSelection(rowIndex: Int, columnIndex: Int, toggle: Boolean, extend: Boolean) {
                val currentMD5 = modifiedLog.getCurrentMD5()
                modifiedLog.setCurrentRowIndex(rowIndex)
                val modifiedEntry = modifiedLog.getModifiedEntry(currentMD5, rowIndex)
                if (modifiedEntry != null) {
                    requestView.request = modifiedEntry.httpRequestResponse.request()
                    responseView.response = modifiedEntry.httpRequestResponse.response()
                    responseView.setSearchExpression(modifiedEntry.diffString)
                }
                super.changeSelection(rowIndex, columnIndex, toggle, extend)
            }
        }

        // 设置修改日志表格属性
        modifiedLogTable.apply {
            autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            columnModel.apply {
                getColumn(0).preferredWidth = 50  // #
                getColumn(1).preferredWidth = 100 // Parameter
                getColumn(2).preferredWidth = 200 // Payload
                getColumn(3).preferredWidth = 100 // Diff
                getColumn(4).preferredWidth = 70  // Status
                getColumn(5).preferredWidth = 70  // Time
            }
        }

        // 对某一列上色
        /**
         *    class CustomTableCellRenderer : DefaultTableCellRenderer() {
         *             override fun getTableCellRendererComponent(
         *                 table: JTable,
         *                 value: Any?,
         *                 isSelected: Boolean,
         *                 hasFocus: Boolean,
         *                 row: Int,
         *                 column: Int,
         *             ): Component {
         *                 // 调用父类的方法以保留默认的渲染行为
         *                 super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
         *
         *                 // 根据列的不同，应用不同的条件逻辑
         *                 when (column) {
         *                     3 -> { // diff列的索引为3
         *                         // 已有的对diff列的逻辑
         *                         when {
         *                             isSelected -> {
         *                                 background = table.selectionBackground
         *                                 foreground = table.selectionForeground
         *                             }
         *
         *                             value is String && value.contains("Error") -> {
         *                                 background = Color.RED
         *                                 foreground = Color.WHITE
         *                             }
         *
         *                             value is String && value.contains("same") -> {
         *                                 background = Color.GRAY
         *                                 foreground = Color.WHITE
         *                             }
         *                             else -> {
         *                                 background = table.background
         *                                 foreground = table.foreground
         *                             }
         *                         }
         *                     }
         *
         *                     4 -> { // status列的索引为4
         *                         // 对status列的新逻辑
         *                         when {
         *                             isSelected -> {
         *                                 background = table.selectionBackground
         *                                 foreground = table.selectionForeground
         *                             }
         *                             value.toString() in listOf("400", "502","500") -> {
         *                                 background = Color.DARK_GRAY
         *                                 foreground = Color.WHITE
         *                             }
         *                             else -> {
         *                                 background = table.background
         *                                 foreground = table.foreground
         *                             }
         *                         }
         *                     }
         *
         *                     else -> { // 其他列的默认处理
         *                         when {
         *                             isSelected -> {
         *                                 background = table.selectionBackground
         *                                 foreground = table.selectionForeground
         *                             }
         *
         *                             else -> {
         *                                 background = table.background
         *                                 foreground = table.foreground
         *                             }
         *                         }
         *                     }
         *                 }
         *                 return this
         *             }
         *         }
         *
         */


        //对整行上色
        class CustomTableCellRenderer : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable,
                value: Any?,
                isSelected: Boolean,
                hasFocus: Boolean,
                row: Int,
                column: Int,
            ): Component {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

                // 定义整行的颜色逻辑
                var background: Color? = null
                var foreground: Color? = null

                if (isSelected) {
                    background = table.selectionBackground
                    foreground = table.selectionForeground
                } else {
                    /**
                     * 通过判断ModifiedLogDataModel的color属性进行染色
                     */
                    val modifiedEntry = modifiedLog.getModifiedEntry(currentMD5, row)
                    background = modifiedEntry?.color?.get(0)
                    foreground = modifiedEntry?.color?.get(1)
                }

                // 如果没有特别设置背景或前景颜色，则使用默认值
                this.background = background ?: table.background
                this.foreground = foreground ?: table.foreground

                return this
            }
        }
        modifiedLogTable.setDefaultRenderer(Object::class.java, CustomTableCellRenderer())


        // 已扫描的表单展示
        val changeRequestResponseTabs = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
            leftComponent = JScrollPane(logTable)
            rightComponent = JScrollPane(modifiedLogTable)
//            dividerLocation = 800
        }
        logViewSplitPanel.leftComponent = changeRequestResponseTabs

        changeRequestResponseTabs.addComponentListener(object : java.awt.event.ComponentAdapter() {
            override fun componentResized(e: java.awt.event.ComponentEvent) {
                //  面板37分
                val newLocation = (changeRequestResponseTabs.width * 0.7).toInt()
                changeRequestResponseTabs.setDividerLocation(newLocation)

            }
        })
        // 添加清除按钮事件
        clearLogsMenuItem.addActionListener {
            synchronized(logs) {

                logs.clear()
                modifiedLog.clear()
                httpInterceptor.clear()
            }
        }

        // 添加右键菜单
        logTable.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    val row = logTable.rowAtPoint(e.point)
                    if (row >= 0) {
                        logTable.changeSelection(row, logTable.columnAtPoint(e.point), false, false)
                        popupMenu.show(e.component, e.x, e.y)
                    }
                }
            }
        })

        // Use the icon directly
        dashBoardPanel.addTab("SQL Scout", logViewSplitPanel)
        dashBoardPanel.addTab("Settings", SettingPanel(dataPersistence))

        return dashBoardPanel
    }
}
