package ui.components

import LogEntryTable
import ModifiedLogTable
import burp.api.montoya.MontoyaApi
import burp.api.montoya.ui.editor.EditorOptions
import config.DataPersistence
import processor.HttpInterceptor
import model.logentry.LogEntry
import model.logentry.ModifiedLogEntry
import processor.GenerateRequests
import java.awt.Color
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableRowSorter

class LogViewPanel(
    private val api: MontoyaApi,
    private val logs: LogEntry,
    private val modifiedLog: ModifiedLogEntry,
    private val httpInterceptor: HttpInterceptor,
    private val dataPersistence: DataPersistence
) {
    private val userInterface = api.userInterface()
    private var currentMD5: String? = null
    private val dashBoardPanel = JTabbedPane()
    fun buildUI(): Component {

        val logViewSplitPanel = JSplitPane(JSplitPane.VERTICAL_SPLIT)
        val requestResponseTabs = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)  //用于展示底部 request 和 response 的数据面板
        // 创建请求响应编辑器
        val requestView = userInterface.createHttpRequestEditor(EditorOptions.READ_ONLY)
        val responseView = userInterface.createHttpResponseEditor(EditorOptions.READ_ONLY)
        requestResponseTabs.leftComponent = requestView.uiComponent()
        requestResponseTabs.rightComponent = responseView.uiComponent()
        logViewSplitPanel.rightComponent = requestResponseTabs //日志展示在底部

        val modifiedLogTable = ModifiedLogTable(modifiedLog,requestView, responseView)

        // 创建右键菜单
        val popupMenu = JPopupMenu()
        val clearLogsMenuItem = JMenuItem("Clear History")
        popupMenu.add(clearLogsMenuItem)

//         主日志表格
        val logTable = object : JTable(logs) {
            init {
                autoCreateRowSorter = true
            }
            override fun changeSelection(rowIndex: Int, columnIndex: Int, toggle: Boolean, extend: Boolean) {
                val modelRow = convertRowIndexToModel(rowIndex)
                currentMD5 = logs.getEntryMD5ByIndex(modelRow).toString()

                // 先重置排序  再更新数据 每次点击原始请求后重置排序
//                modifiedLog.sortByColor()
                modifiedLogTable.resetSorter()
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
                GenerateRequests.cleanData()
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

    fun setTitleColor() {
        if (dashBoardPanel.parent != null) {
            dashBoardPanel.parent.background = Color.RED
        }
    }

}