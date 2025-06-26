//import com.github.difflib.DiffUtils
//import com.github.difflib.patch.Patch
//import java.awt.*
//import java.io.File
//import java.nio.file.Files
//import javax.swing.*
//
//
////import
//
//class LoginFrame : JFrame() {
//    private val logPanel: JPanel = JPanel()
//    private val statusBar: JPanel = JPanel()
//    private var isLogVisible = false
//    private var isStatusBarVisible = false
//
//    init {
//        title = "登录界面"
//        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
//        setSize(400, 300)
//        layout = BorderLayout()
//
//        // 创建登录面板
//        val loginPanel = JPanel()
//        loginPanel.layout = GridBagLayout()
//        val constraints = GridBagConstraints()
//        constraints.insets = Insets(5, 5, 5, 5)
//
//        val usernameLabel = JLabel("用户名：")
//        val usernameField = JTextField(20)
//        val passwordLabel = JLabel("密码：")
//        val passwordField = JPasswordField(20)
//        val loginButton = JButton("登录")
//
//        constraints.gridx = 0
//        constraints.gridy = 0
//        loginPanel.add(usernameLabel, constraints)
//        constraints.gridx = 1
//        loginPanel.add(usernameField, constraints)
//        constraints.gridx = 0
//        constraints.gridy = 1
//        loginPanel.add(passwordLabel, constraints)
//        constraints.gridx = 1
//        loginPanel.add(passwordField, constraints)
//        constraints.gridx = 1
//        constraints.gridy = 2
//        loginPanel.add(loginButton, constraints)
//
//        add(loginPanel, BorderLayout.CENTER)
//
//        // 创建底部面板
//        val bottomPanel = JPanel(BorderLayout())
//        val logButton = JButton("Log")
//        bottomPanel.add(logButton, BorderLayout.WEST)
//
//        // 创建JTable
//        val tableData = arrayOf(
//            arrayOf("列1", "列2", "列3"),
//            arrayOf("数据1", "数据2", "数据3")
//        )
//        val columnNames = arrayOf("列1", "列2", "列3")
//        val table = JTable(tableData, columnNames)
//        val scrollPane = JScrollPane(table)
//        scrollPane.preferredSize = Dimension(380, 100)
//        bottomPanel.add(scrollPane, BorderLayout.CENTER)
//
//        add(bottomPanel, BorderLayout.SOUTH)
//
//        // 创建Log面板
//        logPanel.layout = BorderLayout()
//        logPanel.background = Color.LIGHT_GRAY
//        val logTextArea = JTextArea()
//        logTextArea.isEditable = false
//        logPanel.add(JScrollPane(logTextArea), BorderLayout.CENTER)
//        logPanel.isVisible = false
//        add(logPanel, BorderLayout.NORTH)
//
//        // 创建状态栏
//        statusBar.layout = BorderLayout()
//        statusBar.background = Color.DARK_GRAY
//        val statusLabel = JLabel("状态栏")
//        statusLabel.foreground = Color.WHITE
//        statusBar.add(statusLabel, BorderLayout.CENTER)
//        statusBar.preferredSize = Dimension(width, 30)
//        statusBar.isVisible = false
//        add(statusBar, BorderLayout.NORTH)
//
//        // 添加Log按钮事件
//        logButton.addActionListener {
//            isLogVisible = !isLogVisible
//            logPanel.isVisible = isLogVisible
//            if (isLogVisible) {
//                remove(statusBar)
//                add(logPanel, BorderLayout.NORTH)
//            } else {
//                remove(logPanel)
//                add(statusBar, BorderLayout.NORTH)
//            }
//            revalidate()
//            repaint()
//        }
//
//        // 添加状态栏点击事件
//        statusBar.addMouseListener(object : java.awt.event.MouseAdapter() {
//            override fun mouseClicked(e: java.awt.event.MouseEvent) {
//                isStatusBarVisible = !isStatusBarVisible
//                statusBar.isVisible = isStatusBarVisible
//                revalidate()
//                repaint()
//            }
//        })
//    }
//}
//
////fun utils.main() {
////    SwingUtilities.invokeLater {
////        val frame = LoginFrame()
////        frame.isVisible = true
////    }
//
//
////}
//
//data class USer(
//    val name: String,
//    val email: String,
//)
//fun diffTest(){
//
//    /** diff
//     *
//     */
//    val original = Files.readAllLines(File("/Users/javeley/Documents/Codes/KotlinBurpExtensionBase-master-cursors/src/original.txt").toPath())
//    val revised = Files.readAllLines(File("/Users/javeley/Documents/Codes/KotlinBurpExtensionBase-master-cursors/src/new.txt").toPath())
//    val patch: Patch<String> = DiffUtils.diff(original, revised)
//
//    println(patch)
//    println(patch.deltas[0].target)
//    println(patch.deltas[0].target.lines.removeFirst())
//    println("+21".toIntOrNull())
//}
//
////fun utils.main() {
//
////    println("a=1"[2])
//
////    diffTest()
//
//// 初始化一个包含两个 null 的可变列表
////    val a: MutableList<Color?> = mutableListOf(null, null)
//
//// 使用索引操作符来设置值
////    a[0] = Color.RED
////    a[1] = Color.RED
////    println(a.get(0)!=null)
////
////    println(a.toString())
//
//
//
//
//
//
////    println(patch.deltas[0].toString().split("]to[")
////        [1].trim().dropLast(3))
////    if (patch.deltas.size == 1)
////        println("] to [ ${patch.deltas[0].toString().split("] to [")}")
////        println( patch.deltas[0].toString().split("] to [")[1].trim().dropLast(2))
//    //的到的是You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near ''='"="' at line
////}
//
////data class ModifiedLogDataModel(
////    val vulnerability: Boolean = false
////) : TableModel
////
////fun main() {
////    // 场景1：所有条目都安全
////    val entries1 = listOf(
////        ModifiedLogDataModel(),
////        ModifiedLogDataModel(),
////        ModifiedLogDataModel()
////    )
////    println("场景1结果: ${entries1.all  { it.vulnerability == false }}") // 输出：false
////
////    // 场景2：存在一个漏洞条目
////    val entries2 = listOf(
////        ModifiedLogDataModel(),
////        ModifiedLogDataModel(vulnerability = true),
////        ModifiedLogDataModel()
////    )
////    println("场景2结果: ${entries2.any { it.vulnerability   }}") // 输出：true
////
////    // 场景3：空列表
////    val entries3 = emptyList<ModifiedLogDataModel>()
////    println("场景3结果: ${entries3.any { it.vulnerability }}") // 输出：false
////}
////
