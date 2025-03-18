//import burp.api.montoya.ui.editor.HttpRequestEditor
//import burp.api.montoya.ui.editor.HttpResponseEditor
//import model.logentry.LogEntry
//import model.logentry.ModifiedLogEntry
//import javax.swing.JTable
//import javax.swing.ListSelectionModel
//import javax.swing.SwingUtilities
//import javax.swing.table.TableRowSorter
//
//class LogEntryTable(
//    private val logEntry: LogEntry,
//    private val modifiedLog: ModifiedLogEntry,
//    private val requestView: HttpRequestEditor,
//    private val responseView: HttpResponseEditor
//) : JTable(logEntry) {
//
//    private var currentMD5: String? = null
//        set(value) {
//            field = value
//            value?.let { modifiedLog.setCurrentEntry(it) }
//        }
//
//    init {
//        autoCreateRowSorter = true
//        setupSorting()
//        setupTableProperties()
//    }
//
//    private fun setupTableProperties() {
//        autoResizeMode = AUTO_RESIZE_ALL_COLUMNS
//        selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
//        columnModel.apply {
//            getColumn(0).preferredWidth = 50
//            getColumn(1).preferredWidth = 70
//            getColumn(2).preferredWidth = 150
//            getColumn(3).preferredWidth = 400
//            getColumn(4).preferredWidth = 50
//            getColumn(5).preferredWidth = 70
//            getColumn(6).preferredWidth = 80
//            getColumn(7).preferredWidth = 70
//        }
//    }
//
//    private fun setupSorting() {
//        val sorter = TableRowSorter(model)
//        sorter.setComparator(0) { o1, o2 ->
//            val num1 = o1.toString().toIntOrNull() ?: 0
//            val num2 = o2.toString().toIntOrNull() ?: 0
//            num1.compareTo(num2)
//        }
//        rowSorter = sorter
//    }
//
//    override fun changeSelection(rowIndex: Int, columnIndex: Int, toggle: Boolean, extend: Boolean) {
//        val modelRow = convertRowIndexToModel(rowIndex)
//        currentMD5 = logEntry.getEntryMD5ByIndex(modelRow) ?: return
//        updateViews()
//        super.changeSelection(rowIndex, columnIndex, toggle, extend)
//    }
//
//    private fun updateViews() {
//        if (!SwingUtilities.isEventDispatchThread()) {
//            SwingUtilities.invokeLater { updateViews() }
//            return
//        }
//        currentMD5?.let { md5 ->
//            logEntry.getEntry(md5)?.requestResponse?.let { rr ->
//                requestView.request = rr.request()
//                responseView.response = rr.response()
//            }
//        }
//    }
//}