package ui.components//class SettingPanel : JPanel() {
//    private val configs = Configs.INSTANCE
//
//    init {
//        layout = BorderLayout(10, 10)
//        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
//
//        // 创建主要内容面板
//        val mainPanel = JPanel(GridBagLayout())
//
//        // 添加标题和模式选择
//        addTitlePanel(mainPanel)
//
//        // 添加左侧选项面板
//        addOptionsPanel(mainPanel)
//
//        // 添加右侧预览面板
//        addPreviewPanel(mainPanel)
//
//        add(mainPanel, BorderLayout.CENTER)
//
//        // 添加底部按钮面板
//        addBottomPanel()
//    }
//
//    private fun addTitlePanel(mainPanel: JPanel) {
//        val titlePanel = JPanel(FlowLayout(FlowLayout.LEFT))
//        titlePanel.background = Color(240, 240, 240)
//
//        // 添加标题
//        val titleLabel = JLabel("GAP Extension")
//        titleLabel.font = Font("Arial", Font.BOLD, 14)
//        titlePanel.add(titleLabel)
//
//        // 添加模式选择
//        val modePanel = JPanel(FlowLayout(FlowLayout.LEFT))
//        modePanel.add(JLabel("GAP Mode:"))
//        listOf("Parameters", "Links", "Words").forEach { mode ->
//            val checkbox = JCheckBox(mode)
//            checkbox.isSelected = true
//            modePanel.add(checkbox)
//        }
//        titlePanel.add(modePanel)
//
//        val gbc = GridBagConstraints().apply {
//            gridx = 0
//            gridy = 0
//            gridwidth = 2
//            fill = GridBagConstraints.HORIZONTAL
//            insets = Insets(0, 0, 10, 0)
//        }
//        mainPanel.add(titlePanel, gbc)
//    }
//
//    private fun addOptionsPanel(mainPanel: JPanel) {
//        val optionsPanel = JPanel(GridBagLayout())
//        optionsPanel.border = BorderFactory.createTitledBorder(
//            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
//            "Parameters mode options:"
//        )
//
//        // 请求参数选项
//        val requestPanel = createParameterSection("REQUEST PARAMETERS", listOf(
//            "Query string params",
//            "Message body params",
//            "Param attribute in multi-part message body",
//            "JSON params",
//            "Cookie names",
//            "Items of data in XML structure",
//            "Value of tag attributes in XML structure"
//        ))
//
//        // 响应参数选项
//        val responsePanel = createParameterSection("RESPONSE PARAMETERS", listOf(
//            "JSON params",
//            "Value of tag attributes in XML structure",
//            "Name and Id attributes of HTML input fields",
//            "Javascript variables and constants",
//            "Params from links found"
//        ))
//
//        val gbc = GridBagConstraints().apply {
//            gridx = 0
//            gridy = 0
//            anchor = GridBagConstraints.NORTHWEST
//            fill = GridBagConstraints.HORIZONTAL
//            weightx = 1.0
//            insets = Insets(5, 5, 5, 5)
//        }
//
//        optionsPanel.add(requestPanel, gbc)
//
//        gbc.gridy = 1
//        optionsPanel.add(responsePanel, gbc)
//
//        // 添加到主面板
//        val mainGbc = GridBagConstraints().apply {
//            gridx = 0
//            gridy = 1
//            fill = GridBagConstraints.BOTH
//            weightx = 0.5
//            weighty = 1.0
//            insets = Insets(0, 0, 0, 10)
//        }
//        mainPanel.add(optionsPanel, mainGbc)
//    }
//
//    private fun createParameterSection(title: String, options: List<String>): JPanel {
//        val panel = JPanel(GridBagLayout())
//        panel.border = BorderFactory.createTitledBorder(title)
//
//        val gbc = GridBagConstraints().apply {
//            gridx = 0
//            anchor = GridBagConstraints.WEST
//            fill = GridBagConstraints.HORIZONTAL
//            weightx = 1.0
//            insets = Insets(2, 5, 2, 5)
//        }
//
//        options.forEach { option ->
//            val checkbox = JCheckBox(option)
//            gbc.gridy++
//            panel.add(checkbox, gbc)
//        }
//
//        return panel
//    }
//
//    private fun addPreviewPanel(mainPanel: JPanel) {
//        val previewPanel = JPanel(GridBagLayout())
//        previewPanel.border = BorderFactory.createTitledBorder(
//            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
//            "Preview"
//        )
//
//        // 添加预览内容
//        val textArea = JTextArea()
//        textArea.lineWrap = true
//        textArea.wrapStyleWord = true
//        val scrollPane = JScrollPane(textArea)
//
//        val gbc = GridBagConstraints().apply {
//            fill = GridBagConstraints.BOTH
//            weightx = 1.0
//            weighty = 1.0
//            insets = Insets(5, 5, 5, 5)
//        }
//        previewPanel.add(scrollPane, gbc)
//
//        // 添加到主面板
//        val mainGbc = GridBagConstraints().apply {
//            gridx = 1
//            gridy = 1
//            fill = GridBagConstraints.BOTH
//            weightx = 0.5
//            weighty = 1.0
//        }
//        mainPanel.add(previewPanel, mainGbc)
//    }
//
//    private fun addBottomPanel() {
//        val bottomPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
//
//        val restoreButton = JButton("Restore defaults")
//        val saveButton = JButton("Save options")
//
//        bottomPanel.add(restoreButton)
//        bottomPanel.add(saveButton)
//
//        add(bottomPanel, BorderLayout.SOUTH)
//    }
//}