package ui.components

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import config.Configs
import config.DataPersistence
import java.awt.*
import java.io.*
import java.nio.charset.StandardCharsets
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder
import javax.swing.filechooser.FileNameExtensionFilter

class SettingPanel(private val dataPersistence: DataPersistence) : JPanel() {
    private val configs = dataPersistence.config
    // 配置 Gson，处理特殊字符不转义
    private val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()


    // 顶部开关
    private val cbStartUp = createCheckBox(configs.startUP) { configs.startUP = it; save() }
    private val cbOnlyScope = createCheckBox(configs.isInScope) { configs.isInScope = it; save() }
    private val cbProxy = createCheckBox(configs.proxy) { configs.proxy = it; save() }
    private val cbRepeater = createCheckBox(configs.repeater) { configs.repeater = it; save() }

    // 基础设置
    private val cbNullCheck = createCheckBox(configs.nullCheck) { configs.nullCheck = it; save() }
    private val txtMaxParam = createTextField(configs.maxAllowedParameterCount.toString()) { it.toIntOrNull()?.let { v -> configs.maxAllowedParameterCount = v; save() } }
    private val txtFixedInterval = createTextField(configs.fixedIntervalTime.toString()) { it.toLongOrNull()?.let { v -> configs.fixedIntervalTime = v; save() } }
    private val txtRandomDelay = createTextField(configs.randomCheckTimer.toString()) { it.toLongOrNull()?.let { v -> configs.randomCheckTimer = v; save() } }

    // 过滤设置
    private val txtNeverScanRegex = createTextField(configs.neverScanRegex) { configs.neverScanRegex = it; save() }
    private val txtNestedJsonKey = createTextField(configs.nestedJsonParams) { configs.nestedJsonParams = it; save() }

    private val textAreaMap = mutableMapOf<String, JTextArea>()

    private fun save() {
        dataPersistence.updateConfig()
    }

    object Style {
        val BURP_ORANGE = Color(227, 107, 30)
        val HEADER_BG = Color(90, 80, 70) // 深色背景
        val TEXT_NORMAL = Color(51, 51, 51)
        val TEXT_WHITE = Color(220, 220, 220)
        val BORDER_COLOR = Color(200, 200, 200)

        val FONT_MAIN = Font("SansSerif", Font.PLAIN, 13)
        val FONT_HEADER = FONT_MAIN.deriveFont(Font.BOLD, 16f)
        val FONT_LABEL = FONT_MAIN.deriveFont(Font.BOLD, 13f)

        fun createInputBorder() = BorderFactory.createCompoundBorder(
            LineBorder(BORDER_COLOR, 1, true),
            EmptyBorder(5, 8, 5, 8)
        )
    }

    /**
     * 提示信息
     */
    private val tooltips = mapOf(
        "Null Check:" to "Enable this to check parameters null value different",
        "Max Param Count:" to "Maximum number of parameters to scan in a single request",
        "Fixed Interval (ms):" to "Fixed interval between scan requests in milliseconds",
        "Random Delay (ms):" to "Additional random delay added to fixed interval for each request",
        "Never Scan URI Regex:" to "URLs matching these regular expressions will be skipped (e.g.'(delete|logout)' )",
        "Nested JSON Key:" to "Input parameters containing nested JSON strings (e.g.'biz_content').",
        "SQL Payloads" to "SQL injection payloads to test against parameters",
        "Boring Words" to "Boring words that will be excluded in scan",
        "Ignore Params" to "Ignore parameters that will be passed in",
        "MIME Types" to "MIME types that will be included in scan",
        "Skip Exts" to "File extensions that will be skipped during scanning"
    )

    init {
        layout = BorderLayout()
        background = Color.WHITE

        val mainPanel = JPanel(GridBagLayout())
        mainPanel.background = Color.WHITE
        mainPanel.border = EmptyBorder(0, 0, 0, 0)

        addTitlePanel(mainPanel)

        val contentPanel = JPanel(GridBagLayout())
        contentPanel.background = Color.WHITE
        contentPanel.border = EmptyBorder(20, 20, 20, 20)

        addParametersPanel(contentPanel)
        addRightPanel(contentPanel)

        val gbc = GridBagConstraints().apply {
            gridx = 0
            gridy = 1
            weightx = 1.0
            weighty = 1.0
            fill = GridBagConstraints.BOTH
        }
        mainPanel.add(contentPanel, gbc)
        add(mainPanel, BorderLayout.CENTER)
    }

    private fun addTitlePanel(mainPanel: JPanel) {
        val titlePanel = JPanel(BorderLayout())
        titlePanel.background = Style.HEADER_BG
        titlePanel.border = EmptyBorder(15, 20, 15, 20)

        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 15, 0))
        leftPanel.isOpaque = false
        try {
            val iconStream = javaClass.getResourceAsStream("/icon.jpeg")
            if (iconStream != null) {
                val icon = ImageIcon(ImageIO.read(iconStream))
                val scaledIcon = icon.image.getScaledInstance(32, 32, Image.SCALE_SMOOTH)
                leftPanel.add(JLabel(ImageIcon(scaledIcon)))
            }
        } catch (e: Exception) { e.printStackTrace() }

        val titleLabel = JLabel("SQL Injection Scout").apply {
            font = Style.FONT_HEADER
            foreground = Style.BURP_ORANGE
        }
        val subTitleLabel = JLabel(" extension by JaveleyQAQ").apply {
            font = Style.FONT_MAIN
            foreground = Color.GRAY
        }
        val textContainer = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            isOpaque = false
            add(titleLabel)
            add(subTitleLabel)
        }
        leftPanel.add(textContainer)
        titlePanel.add(leftPanel, BorderLayout.WEST)

        val optionsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 15, 0))
        optionsPanel.isOpaque = false

        // === 按钮样式 ===
        val btnStyle = Font("SansSerif", Font.BOLD, 11)
        val btnColor = Color(70, 70, 70)

        // Init 按钮
        val btnInit = JButton("Init").apply {
            background = Color(200, 60, 60)
            foreground = Color.WHITE
            isFocusPainted = false
            font = btnStyle
            toolTipText = "Reset configuration to defaults"
            addActionListener { performInit() }
        }

        val btnExport = JButton("Export").apply {
            background = btnColor
            foreground = Color.WHITE
            isFocusPainted = false
            font = btnStyle
            addActionListener { performExport() }
        }

        val btnImport = JButton("Import").apply {
            background = btnColor
            foreground = Color.WHITE
            isFocusPainted = false
            font = btnStyle
            addActionListener { performImport() }
        }

        optionsPanel.add(btnInit)
        optionsPanel.add(Box.createHorizontalStrut(5))
        optionsPanel.add(btnExport)
        optionsPanel.add(Box.createHorizontalStrut(5))
        optionsPanel.add(btnImport)
        optionsPanel.add(Box.createHorizontalStrut(15))

        fun addCb(text: String, cb: JCheckBox) {
            cb.text = text
            cb.foreground = Style.TEXT_WHITE
            cb.background = Style.HEADER_BG
            cb.font = Style.FONT_MAIN
            cb.isFocusPainted = false
            optionsPanel.add(cb)
        }
        addCb("StartUP", cbStartUp)
        addCb("Only Scope", cbOnlyScope)
        addCb("Proxy", cbProxy)
        addCb("Repeater", cbRepeater)

        titlePanel.add(optionsPanel, BorderLayout.EAST)

        val gbc = GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            gridwidth = 2
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
        }
        mainPanel.add(titlePanel, gbc)
    }

    private fun addParametersPanel(parent: JPanel) {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.background = Color.WHITE

        val basicGroup = createGroupPanel("Basic Settings")
        addConfigRow(basicGroup, "Null Check:", cbNullCheck)
        addConfigRow(basicGroup, "Max Param Count:", txtMaxParam)
        addConfigRow(basicGroup, "Fixed Interval (ms):", txtFixedInterval)
        addConfigRow(basicGroup, "Random Delay (ms):", txtRandomDelay)
        panel.add(basicGroup)
        panel.add(Box.createVerticalStrut(15))

        val filterGroup = createGroupPanel("Filter Configuration")
        addConfigRow(filterGroup, "Never Scan URI Regex:", txtNeverScanRegex)
        addConfigRow(filterGroup, "Nested JSON Key:", txtNestedJsonKey)
        panel.add(filterGroup)
        panel.add(Box.createVerticalStrut(15))

        val listGroup = createGroupPanel("Payloads & Wordlists")
        val tabbedPane = JTabbedPane()
        tabbedPane.font = Style.FONT_MAIN

        fun addTabWithTooltip(title: String, list: MutableList<String>) {
            val scrollPane = createScrollTextArea(list) { lines ->
                list.clear()
                list.addAll(lines)
                save()
            }
            // 记录组件引用，方便刷新
            val textArea = scrollPane.viewport.view as JTextArea
            textAreaMap[title] = textArea

            tabbedPane.addTab(title, scrollPane)
            val tooltipText = tooltips[title] ?: tooltips["$title:"]
            tooltipText?.let { tabbedPane.setToolTipTextAt(tabbedPane.tabCount - 1, it) }
        }

        addTabWithTooltip("SQL Payloads", configs.payloads)
        addTabWithTooltip("Boring Words", configs.boringWords)
        addTabWithTooltip("Ignore Params", configs.ignoreParams)
        addTabWithTooltip("MIME Types", configs.allowedMimeTypeMimeType)
        addTabWithTooltip("Skip Exts", configs.uninterestingType)

        listGroup.add(tabbedPane)
        panel.add(listGroup)

        val gbc = GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            weightx = 0.65
            weighty = 1.0
            fill = GridBagConstraints.BOTH
            insets = Insets(0, 0, 0, 10)
        }
        parent.add(panel, gbc)
    }

    private fun addRightPanel(parent: JPanel) {
        val panel = createGroupPanel("Preview / Hidden Params")
        panel.background = Color.WHITE

        val scrollPane = createScrollTextArea(configs.hiddenParams) { list ->
            configs.hiddenParams.clear()
            configs.hiddenParams.addAll(list)
            save()
        }
        val textArea = scrollPane.viewport.view as JTextArea
        textAreaMap["Hidden Params"] = textArea

        panel.add(scrollPane)

        val gbc = GridBagConstraints().apply {
            gridx = 1
            gridy = 0
            weightx = 0.35
            weighty = 1.0
            fill = GridBagConstraints.BOTH
            insets = Insets(0, 10, 0, 0)
        }
        parent.add(panel, gbc)
    }

    private fun performInit() {
        val result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to reset all configurations to default?\nThis action cannot be undone.",
            "Confirm Reset",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        )

        if (result == JOptionPane.YES_OPTION) {
            try {
                val defaults = Configs()
                updateConfigSingleton(defaults)
                refreshUIFromConfig()
                save()
                JOptionPane.showMessageDialog(this, "Configuration reset to defaults.", "Success", JOptionPane.INFORMATION_MESSAGE)
            } catch (e: Exception) {
                e.printStackTrace()
                JOptionPane.showMessageDialog(this, "Reset failed: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
            }
        }
    }

    private fun performExport() {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Export Config"
        fileChooser.selectedFile = File("SQLScout_Config.json")
        fileChooser.fileFilter = FileNameExtensionFilter("JSON Config", "json")

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                val file = fileChooser.selectedFile
                val targetFile = if (file.name.endsWith(".json")) file else File("${file.path}.json")

                OutputStreamWriter(FileOutputStream(targetFile), StandardCharsets.UTF_8).use { writer ->
                    gson.toJson(configs, writer)
                }
                JOptionPane.showMessageDialog(this, "Export successful!", "Success", JOptionPane.INFORMATION_MESSAGE)
            } catch (e: Exception) {
                e.printStackTrace()
                JOptionPane.showMessageDialog(this, "Export failed: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
            }
        }
    }

    private fun performImport() {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Import Config"
        fileChooser.fileFilter = FileNameExtensionFilter("JSON Config", "json")

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                InputStreamReader(FileInputStream(fileChooser.selectedFile), StandardCharsets.UTF_8).use { reader ->
                    val newConfig = gson.fromJson(reader, Configs::class.java)
                    if (newConfig == null) throw Exception("Failed to parse JSON")

                    updateConfigSingleton(newConfig)
                    refreshUIFromConfig()
                    save()
                }
                JOptionPane.showMessageDialog(this, "Import successful!", "Success", JOptionPane.INFORMATION_MESSAGE)
            } catch (e: Exception) {
                e.printStackTrace()
                JOptionPane.showMessageDialog(this, "Import failed: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
            }
        }
    }

    /**
     * 安全地更新 Configs 单例
     */
    private fun updateConfigSingleton(newConfig: Configs) {
        configs.startUP = newConfig.startUP
        configs.isInScope = newConfig.isInScope
        configs.proxy = newConfig.proxy
        configs.repeater = newConfig.repeater
        configs.nullCheck = newConfig.nullCheck

        configs.maxAllowedParameterCount = newConfig.maxAllowedParameterCount
        configs.fixedIntervalTime = newConfig.fixedIntervalTime
        configs.randomCheckTimer = newConfig.randomCheckTimer

        configs.neverScanRegex = newConfig.neverScanRegex ?: ""
        configs.nestedJsonParams = newConfig.nestedJsonParams ?: ""

        fun updateList(target: MutableList<String>, source: List<String>?) {
            if (source != null) {
                target.clear()
                target.addAll(source)
            }
        }

        updateList(configs.payloads, newConfig.payloads)
        updateList(configs.boringWords, newConfig.boringWords)
        updateList(configs.ignoreParams, newConfig.ignoreParams)
        updateList(configs.allowedMimeTypeMimeType, newConfig.allowedMimeTypeMimeType)
        updateList(configs.uninterestingType, newConfig.uninterestingType)
        updateList(configs.hiddenParams, newConfig.hiddenParams)
    }

    /**
     * 刷新界面显示
     */
    private fun refreshUIFromConfig() {
        SwingUtilities.invokeLater {
            cbStartUp.isSelected = configs.startUP
            cbOnlyScope.isSelected = configs.isInScope
            cbProxy.isSelected = configs.proxy
            cbRepeater.isSelected = configs.repeater
            cbNullCheck.isSelected = configs.nullCheck

            txtMaxParam.text = configs.maxAllowedParameterCount.toString()
            txtFixedInterval.text = configs.fixedIntervalTime.toString()
            txtRandomDelay.text = configs.randomCheckTimer.toString()
            txtNeverScanRegex.text = configs.neverScanRegex
            txtNestedJsonKey.text = configs.nestedJsonParams

            textAreaMap["SQL Payloads"]?.text = configs.payloads.joinToString("\n")
            textAreaMap["Boring Words"]?.text = configs.boringWords.joinToString("\n")
            textAreaMap["Ignore Params"]?.text = configs.ignoreParams.joinToString("\n")
            textAreaMap["MIME Types"]?.text = configs.allowedMimeTypeMimeType.joinToString("\n")
            textAreaMap["Skip Exts"]?.text = configs.uninterestingType.joinToString("\n")
            textAreaMap["Hidden Params"]?.text = configs.hiddenParams.joinToString("\n")
        }
    }

    /**
     * UI工厂
     */
    private fun createGroupPanel(title: String): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.background = Color.WHITE
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                title,
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                Style.FONT_LABEL,
                Style.BURP_ORANGE
            ),
            EmptyBorder(10, 5, 10, 5)
        )
        return panel
    }

    private fun addConfigRow(panel: JPanel, labelText: String, component: JComponent) {
        val row = JPanel(BorderLayout(10, 0))
        row.background = Color.WHITE
        row.maximumSize = Dimension(Int.MAX_VALUE, 35)
        row.border = EmptyBorder(0, 0, 5, 0)

        val label = JLabel(labelText).apply {
            font = Style.FONT_MAIN
            foreground = Style.TEXT_NORMAL
            preferredSize = Dimension(160, 30)
            horizontalAlignment = SwingConstants.RIGHT
            tooltips[labelText]?.let { toolTipText = it }
        }

        row.add(label, BorderLayout.WEST)
        row.add(component, BorderLayout.CENTER)
        panel.add(row)
        panel.add(Box.createVerticalStrut(5))
    }

    private fun createTextField(initialValue: String, onChange: (String) -> Unit): JTextField {
        return JTextField(initialValue).apply {
            font = Style.FONT_MAIN
            border = Style.createInputBorder()
            document.addDocumentListener(SimpleDocumentListener {
                onChange(text.trim())
            })
        }
    }

    private fun createCheckBox(initialValue: Boolean, onChange: (Boolean) -> Unit): JCheckBox {
        return JCheckBox().apply {
            isSelected = initialValue
            background = Color.WHITE
            addActionListener { onChange(isSelected) }
        }
    }

    private fun createScrollTextArea(dataList: MutableList<String>, onUpdate: (List<String>) -> Unit): JScrollPane {
        val textArea = JTextArea(dataList.joinToString("\n")).apply {
            font = Font("Monospaced", Font.PLAIN, 12)
            lineWrap = true
            wrapStyleWord = true
            border = EmptyBorder(5, 5, 5, 5)
            document.addDocumentListener(SimpleDocumentListener {
                val lines = text.lines().filter { it.isNotBlank() }
                onUpdate(lines)
            })
        }
        return JScrollPane(textArea).apply {
            border = Style.createInputBorder()
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        }
    }

    class SimpleDocumentListener(val onUpdate: () -> Unit) : javax.swing.event.DocumentListener {
        private var timer: Timer? = null
        private fun debounce() {
            timer?.stop()
            timer = Timer(300) { onUpdate() }.apply { isRepeats = false; start() }
        }
        override fun insertUpdate(e: javax.swing.event.DocumentEvent) = debounce()
        override fun removeUpdate(e: javax.swing.event.DocumentEvent) = debounce()
        override fun changedUpdate(e: javax.swing.event.DocumentEvent) = debounce()
    }
}