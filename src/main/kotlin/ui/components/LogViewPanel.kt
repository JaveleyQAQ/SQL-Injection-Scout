package ui.components

import burp.api.montoya.MontoyaApi
import burp.api.montoya.ui.editor.EditorOptions
import burp.api.montoya.ui.editor.HttpRequestEditor
import burp.api.montoya.ui.editor.HttpResponseEditor
import config.DataPersistence
import model.logentry.LogEntry
import model.logentry.ModifiedLogEntry
import processor.helper.payload.GenerateRequests
import processor.http.HttpInterceptor
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer

class LogViewPanel(
    val api: MontoyaApi,
    private val logs: LogEntry,
    private val modifiedLog: ModifiedLogEntry,
    private val httpInterceptor: HttpInterceptor,
    private val dataPersistence: DataPersistence
) {
    private val userInterface = api.userInterface()
    private var currentMD5: String? = null
    private val dashBoardPanel = JTabbedPane()

    private object Style {
        val BURP_ORANGE = Color(227, 107, 30)
        val HEADER_BG = Color(90, 80, 70)
        val TABLE_HEADER_FONT = Font("SansSerif", Font.BOLD, 13)
        val TABLE_FONT = Font("SansSerif", Font.PLAIN, 13)
        val BORDER_COLOR = Color(200, 200, 200)
    }

    fun buildUI(): Component {
        // 1. 创建编辑器 (Request / Response)
        val requestView = userInterface.createHttpRequestEditor(EditorOptions.READ_ONLY)
        val responseView = userInterface.createHttpResponseEditor(EditorOptions.READ_ONLY)

        // 2. 创建表格组件
        val modifiedLogTable = ModifiedLogTable(modifiedLog, requestView, responseView)
        modifiedLogTable.font = Style.TABLE_FONT
        modifiedLogTable.rowHeight = 22
        val logTable = createMainLogTable(requestView, responseView, modifiedLogTable)

        // 3. 组装面板
        // 底部 Request/Response 面板
        val requestResponseTabs = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
            leftComponent = requestView.uiComponent()
            rightComponent = responseView.uiComponent()
            resizeWeight = 0.5 // 左右均分
            border = BorderFactory.createEmptyBorder() // 去除多余边框
        }

        // 中间 日志列表面板 (左: 主日志, 右: 变异日志)
        val logsSplitPanel = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
            leftComponent = JScrollPane(logTable).apply { border = BorderFactory.createLineBorder(Style.BORDER_COLOR) }
            rightComponent = JScrollPane(modifiedLogTable).apply { border = BorderFactory.createLineBorder(Style.BORDER_COLOR) }
            resizeWeight = 0.7 // 核心优化：使用 resizeWeight 代替 ComponentListener，保持 7:3 比例且允许拖动
            dividerSize = 4
        }

        // 整体垂直分割 (上: 日志列表, 下: 详情)
        val mainSplitPanel = JSplitPane(JSplitPane.VERTICAL_SPLIT).apply {
            topComponent = logsSplitPanel
            bottomComponent = requestResponseTabs
            resizeWeight = 0.6 // 上面占 60%
            dividerSize = 4
        }

        // 4. 添加 Tab
        dashBoardPanel.addTab("SQL Scout Logs", mainSplitPanel)
        dashBoardPanel.addTab("Settings", SettingPanel(dataPersistence))

        dashBoardPanel.font = Style.TABLE_HEADER_FONT
        return dashBoardPanel
    }

    private fun createMainLogTable(
        requestView: HttpRequestEditor,
        responseView: HttpResponseEditor,
        modifiedLogTable: ModifiedLogTable // 传入引用以进行联动
    ): JTable {
        val table = object : JTable(logs) {
            init {
                autoCreateRowSorter = true
            }

            override fun changeSelection(rowIndex: Int, columnIndex: Int, toggle: Boolean, extend: Boolean) {
                super.changeSelection(rowIndex, columnIndex, toggle, extend)
                if (rowIndex !in 0..<rowCount)  return else currentMD5 = null
                val modelRow = convertRowIndexToModel(rowIndex)
                val md5 = logs.getEntryMD5ByIndex(modelRow).toString()

                if (currentMD5 != md5) {
                    currentMD5 = md5
                    modifiedLog.setCurrentEntry(currentMD5!!)

                    // UI 更新放入 EDT
                    SwingUtilities.invokeLater {
                        modifiedLog.sortByColor()
                        modifiedLogTable.resetSortToDefault()
                        modifiedLogTable.repaint()
                        modifiedLogTable.rowSorter?.allRowsChanged()
                    }

                    val requestResponse = logs.getEntry(currentMD5!!)?.requestResponse
                    if (requestResponse != null) {
                        requestView.request = requestResponse.request()
                        responseView.response = requestResponse.response()
                    }
                }
            }
        }

        // 配置表格样式
        table.apply {
            autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            font = Style.TABLE_FONT
            rowHeight = 22
            gridColor = Color(230, 230, 230)
            tableHeader.font = Style.TABLE_HEADER_FONT
            tableHeader.background = Color(240, 240, 240)

            columnModel.apply {
                getColumn(0).preferredWidth = 50  // #
                getColumn(1).preferredWidth = 70  // Method
                getColumn(2).preferredWidth = 150 // Host
                getColumn(3).preferredWidth = 400 // Path
                getColumn(4).preferredWidth = 50  // Status
                getColumn(5).preferredWidth = 70  // Body Length
                getColumn(6).preferredWidth = 80  // MIME Type
                getColumn(7).preferredWidth = 70  // Flag
            }
        }

        // 配置自定义渲染器 (序号列居中)
        table.getColumn("#").cellRenderer = object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
            ): Component {
                super.getTableCellRendererComponent(table, (row + 1).toString(), isSelected, hasFocus, row, column)
                horizontalAlignment = SwingConstants.CENTER
                return this
            }
        }

        // 添加右键菜单
        val popupMenu = createPopupMenu(table)
        table.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    val row = table.rowAtPoint(e.point)
                    if (row >= 0) {
                        table.setRowSelectionInterval(row, row) // 右键点击时自动选中该行
                        popupMenu.show(e.component, e.x, e.y)
                    }
                }
            }
        })

        return table
    }

    private fun createPopupMenu(table: JTable): JPopupMenu {
        val popupMenu = JPopupMenu()

        val clearLogsMenuItem = JMenuItem("Clear History")
        clearLogsMenuItem.addActionListener {
            synchronized(logs) {
                logs.clear()
                modifiedLog.clear()
                GenerateRequests.cleanData()
            }
            // 建议：清空后请求视图也应该重置
        }

        val deleteSelectedItem = JMenuItem("Delete Selected Item")
        deleteSelectedItem.addActionListener {
            val row = table.selectedRow
            if (row >= 0) {
                val modelRow = table.convertRowIndexToModel(row)
                synchronized(logs) {
                    logs.deleteSelectedItem(modelRow)
                }
            }
        }

        popupMenu.add(clearLogsMenuItem)
        popupMenu.addSeparator() // 添加分割线
        popupMenu.add(deleteSelectedItem)

        return popupMenu
    }

}