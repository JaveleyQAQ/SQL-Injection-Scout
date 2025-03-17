package ui.components


import java.awt.*
import javax.swing.*
import javax.imageio.ImageIO
import java.awt.Image
import javax.swing.ImageIcon
import config.DataPersistence

/**
 * 设置面板类 - 提供插件的主要配置界面
 * 包含SQL注入测试的各项配置，如Payload、MIME类型、文件扩展名等
 */
class SettingPanel(private val dataPersistence: DataPersistence) : JPanel() {
    private val configs = dataPersistence.config  // 使用 dataPersistence 中的 config

    private val COLOR_BURP_ORANGE = Color(0xE36B1E)  // Burp Suite特色橙色
    private val FONT_FAMILY = " "                 // 字体族
    private val FONT_SIZE = 14                        // 基础字体大小
    // 定义不同用途的字体
    private val FONT_HEADER = Font(FONT_FAMILY, Font.BOLD, FONT_SIZE + 2)  // 标题字体
    private val FONT_HELP = Font(FONT_FAMILY, Font.BOLD, FONT_SIZE)        // 帮助文本字体
    private val FONT_MODE = Font(FONT_FAMILY, Font.BOLD, FONT_SIZE)    // 模式字体
    private val FONT_OPTIONS = Font(FONT_FAMILY, Font.PLAIN, FONT_SIZE - 2)  // 选项字体

    // 创建一个Map来存储标签和对应的提示文本
    private val tooltips = mapOf(
        "Null Check:" to "Enable this to check parameters null value different",
        "Max Param Count:" to "Maximum number of parameters to scan in a single request",
        "FixedInterval(ms):" to "Fixed interval between scan requests in milliseconds",
        "Random Delay Scan:" to "Additional random delay added to fixed interval for each request",
        "Never Scan URLs Matching Regex:" to "URLs matching these regular expressions will be skipped",
        "HeuristicWords" to "Keywords used to identify potential Boring in responses",
        "SQL Payloads:" to "SQL injection payloads to test against parameters",
        "Never Scan Extensions:" to "File extensions that will be skipped during scanning",
        "Scan MIME Types:" to "MIME types that will be included in scanning",
        "Boring Words:" to "Boring words that will be excluded in scan"
    )

    /**
     * 初始化设置面板
     */
    init {
        // 基本布局设置保持不变
        layout = BorderLayout(10, 10)
        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        preferredSize = Dimension(800, 600)
        minimumSize = preferredSize
        maximumSize = preferredSize
        
        // 创建主面板
        val mainPanel = JPanel(GridBagLayout())
        add(mainPanel, BorderLayout.CENTER)

        addTitlePanel(mainPanel)
        addParametersPanel(mainPanel)
        addRightPanel(mainPanel)
    }

    /**
     * 添加标题面板
     * 包含插件标题和功能开关
     * @param mainPanel 主面板，用于添加标题面板
     */
    private fun addTitlePanel(mainPanel: JPanel) {
        // 创建标题面板
        val titlePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val titleBackground = Color(1, 11, 70)  // 深蓝色背景
        titlePanel.background = titleBackground

        // 添加插件标题
        val titleLabel = JLabel("SQL Injection Scout Burp Extension by JaveleyQAQ")
        titleLabel.font = FONT_HEADER
        titleLabel.foreground = COLOR_BURP_ORANGE
        titlePanel.add(titleLabel)

        // 创建模式选择面板
        val modePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        modePanel.background = titleBackground

        // 加载并添加图标
        try {
            // 从资源中加载图标
            val iconStream = javaClass.getResourceAsStream("/icon.jpeg")
            if (iconStream != null) {
                val icon = ImageIcon(ImageIO.read(iconStream))
                // 调整图标大小（根据需要调整尺寸）
                val scaledIcon = icon.image.getScaledInstance(30, 30, Image.SCALE_SMOOTH)
                val logoLabel = JLabel(ImageIcon(scaledIcon))
                modePanel.add(logoLabel)
            } else {
                // 如果图标加载失败，使用文本作为后备
                modePanel.add(JLabel("SQL Scout").apply { 
                    foreground = Color.RED 
                    font = FONT_MODE
                })
            }
        } catch (e: Exception) {
            // 如果出现任何错误，使用文本作为后备
            println("Error loading icon: ${e.message}")
            modePanel.add(JLabel("SQL Scout").apply { 
                foreground = Color.RED 
                font = FONT_MODE
            })
        }

        // 配置复选框选项
        val checkboxConfigs = mapOf(
            "StartUP" to { configs.startUP to { v: Boolean -> configs.startUP = v } },
            "Only Scope" to { configs.isInScope to { v: Boolean -> configs.isInScope = v } },
            "Proxy" to { configs.proxy to { v: Boolean -> configs.proxy = v } },
            "Repeater" to { configs.repeater to { v: Boolean -> configs.repeater = v } }
        )

        // 创建并配置复选框
        checkboxConfigs.forEach { (text, getterAndSetter) ->
            val (initialValue, setter) = getterAndSetter()
            val checkbox = JCheckBox(text).apply {
                isSelected = initialValue
                foreground = Color.PINK
                background = titleBackground
                font = Font(FONT_FAMILY, Font.PLAIN, 14)
                // 设置文本和表情
                this.text = if (initialValue) "$text" else "$text😢"
                
                // 添加动作监听器
                addActionListener {
                    setter(isSelected)
                    this.text = if (isSelected) "$text" else "$text😢"
                    dataPersistence.updateConfig()
//                    println("配置 $text 已更改为: $isSelected")
                }
            }
            modePanel.add(checkbox)
        }
        titlePanel.add(modePanel)

        // 设置标题面板在主面板中的位置
        val gbc = GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            gridwidth = 2
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(0, 0, 10, 0)
        }
        mainPanel.add(titlePanel, gbc)
    }

    /**
     * 添加参数配置面板
     * 包含各种配置项的主要区域
     * @param mainPanel 主面板，用于添加参数面板
     */
    private fun addParametersPanel(mainPanel: JPanel) {
        // 创建参数面板
        val paramsPanel = JPanel(BorderLayout(5, 5))
        // 设置固定宽度（60%的总宽度）
        paramsPanel.preferredSize = Dimension(480, 0)
        paramsPanel.minimumSize = paramsPanel.preferredSize
        paramsPanel.maximumSize = paramsPanel.preferredSize
        
        // 设置边框和标题
        paramsPanel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Configuration"),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        )

        // 创建配置面板，使用BoxLayout垂直排列
        val configPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }

        // 添加基本设置部分
        addConfigSection(configPanel, " ", listOf(
            "Null Check:" to JCheckBox().apply {
                maximumSize = Dimension(100, 25)
                preferredSize = Dimension(100, 25)
                isSelected = configs.nullCheck
                addActionListener {
                    configs.nullCheck = isSelected
                    dataPersistence.updateConfig()
                }
            },
            "Max Param Count:" to JTextField(configs.maxAllowedParameterCount.toString(), 8).apply {
                maximumSize = Dimension(100, 25)
                preferredSize = Dimension(100, 25)
                document.addDocumentListener(object : javax.swing.event.DocumentListener {
                    override fun insertUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()
                    override fun removeUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()
                    override fun changedUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()

                    private fun updateConfig() {
                        text.toIntOrNull()?.let {
                            configs.maxAllowedParameterCount = it
                            dataPersistence.updateConfig()
                        }
                    }
                })
            },
            "FixedInterval(ms):" to JTextField(configs.fixedIntervalTime.toString(), 8).apply {
                maximumSize = Dimension(100, 25)
                preferredSize = Dimension(100, 25)
                document.addDocumentListener(object : javax.swing.event.DocumentListener {
                    override fun insertUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()
                    override fun removeUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()
                    override fun changedUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()

                    private fun updateConfig() {
                        text.toLongOrNull()?.let { configs.fixedIntervalTime = it
                            dataPersistence.updateConfig()}
                    }
                })
            },
            "Random Delay Scan:" to JTextField(configs.randomCheckTimer.toString(), 8).apply {
                maximumSize = Dimension(100, 25)
                preferredSize = Dimension(100, 25)
                document.addDocumentListener(object : javax.swing.event.DocumentListener {
                    override fun insertUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()
                    override fun removeUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()
                    override fun changedUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()

                    private fun updateConfig() {
                        text.toLongOrNull()?.let { configs.randomCheckTimer = it
                            dataPersistence.updateConfig()
                        }
                    }
                })
            },

            "Never Scan URLs Matching Regex:" to JTextField(configs.neverScanRegex.toString(), 8).apply {
                maximumSize = Dimension(100, 25)
                preferredSize = Dimension(100, 25)
                document.addDocumentListener(object : javax.swing.event.DocumentListener {
                    override fun insertUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()
                    override fun removeUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()
                    override fun changedUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()

                    private fun updateConfig() {
                        val newText = text.trim()
                        configs.neverScanRegex = (if (newText.isBlank()) "" else newText).toString()
                        dataPersistence.updateConfig()
                    }
                })
            },

            "SQL Payloads:" to JScrollPane(JTextArea().apply {
                rows = 10
                columns = 30
                lineWrap = true
                wrapStyleWord = true
                font = FONT_OPTIONS
                text = configs.payloads.joinToString("\n")
                border = BorderFactory.createLineBorder(Color.LIGHT_GRAY)
                // 添加文档监听器
                document.addDocumentListener(object : javax.swing.event.DocumentListener {
                    override fun insertUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()
                    override fun removeUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()
                    override fun changedUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()
                    private fun updateConfig() {
                        configs.payloads.clear()
                        // 获取 JTextArea 的文本，并按行分割
                        val text = text.trim()
                        if (text.isNotEmpty()) {
                            // 分割并过滤掉空白行
                            val newPayloads = text.lines().filter { it.isNotBlank() }
                            configs.payloads.clear()
                            configs.payloads.addAll(newPayloads)
                            dataPersistence.updateConfig()
                        }
                    }
                })
            }).apply {
                preferredSize = Dimension(350, 150)
                minimumSize = preferredSize
                maximumSize = preferredSize
            },

            "Boring Words" to JScrollPane(JTextArea().apply {
                rows = 10
                columns = 30
                lineWrap = true
                wrapStyleWord = true
                font = FONT_OPTIONS
                text = configs.boringWords.joinToString("\n")
                border = BorderFactory.createLineBorder(Color.LIGHT_GRAY)
                // 添加文档监听器
                document.addDocumentListener(object : javax.swing.event.DocumentListener {
                    override fun insertUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()
                    override fun removeUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()
                    override fun changedUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()

                    private fun updateConfig() {
                        configs.boringWords.clear()
                        val text = text.trim()
                        if (text.isNotEmpty()) {
                            // 分割并过滤掉空白行
                            val new = text.lines().filter { it.isNotBlank() }
                            configs.boringWords.clear()
                            configs.boringWords.addAll(new)
                            dataPersistence.updateConfig()
                        }
                    }
                })
            }),

            "Never Scan Extensions:" to JScrollPane(JTextArea().apply {
                rows = 10
                columns = 30
                lineWrap = true
                wrapStyleWord = true
                font = FONT_OPTIONS
                text = configs.uninterestingType.joinToString("\n")
                border = BorderFactory.createLineBorder(Color.LIGHT_GRAY)
                // 添加文档监听器，实时更新配置
                document.addDocumentListener(object : javax.swing.event.DocumentListener {
                    override fun insertUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()
                    override fun removeUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()
                    override fun changedUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()
                    private fun updateConfig() {
                        configs.uninterestingType.clear()
                        configs.uninterestingType.addAll(text.lines())
                        dataPersistence.updateConfig()
                    }
                })
            }).apply {
                preferredSize = Dimension(350, 150)
                minimumSize = preferredSize
                maximumSize = preferredSize
            },
            "Scan MIME Types:" to JScrollPane(JTextArea().apply {
                rows = 10
                columns = 30
                lineWrap = true
                wrapStyleWord = true
                font = FONT_OPTIONS
                text = configs.allowedMimeTypeMimeType.joinToString("\n")
                border = BorderFactory.createLineBorder(Color.LIGHT_GRAY)

                // 添加文档监听器
                document.addDocumentListener(object : javax.swing.event.DocumentListener {
                    override fun insertUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()
                    override fun removeUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()
                    override fun changedUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()

                    private fun updateConfig() {
                        configs.allowedMimeTypeMimeType.clear()
                        configs.allowedMimeTypeMimeType.addAll(text.lines())
                        dataPersistence.updateConfig()
                    }
                })
            }).apply {
                preferredSize = Dimension(350, 150)
                minimumSize = preferredSize
                maximumSize = preferredSize
            })
        )

        // 将配置面板添加到滚动面板中
        val scrollPane = JScrollPane(configPanel).apply {
            border = BorderFactory.createEmptyBorder()
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        }
//
        // 添加一个可伸缩的面板容器
        val stretchPanel = JPanel(BorderLayout()).apply {
            add(scrollPane, BorderLayout.CENTER)
            // 添加左右边距，但允许内容伸缩
            add(Box.createHorizontalStrut(10), BorderLayout.WEST)
            add(Box.createHorizontalStrut(10), BorderLayout.EAST)
        }
        paramsPanel.add(stretchPanel, BorderLayout.CENTER)

        // 添加参数面板到主面板
        val panelGbc = GridBagConstraints().apply {
            gridx = 0
            gridy = 1
            fill = GridBagConstraints.BOTH
            weightx = 0.6
            weighty = 1.0
            insets = Insets(0, 0, 0, 10)
        }
        mainPanel.add(paramsPanel, panelGbc)
    }

    /**
     * 添加配置分区的辅助方法
     */
    private fun addConfigSection(panel: JPanel, title: String, items: List<Pair<String, JComponent>>) {
        // 添加分区标题
        panel.add(JLabel(title).apply {
            font = FONT_HEADER
            border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
            alignmentX = LEFT_ALIGNMENT
        })

        // 遍历配置项
        items.forEach { (label, component) ->
            val itemPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                alignmentX = LEFT_ALIGNMENT
                maximumSize = Dimension(Short.MAX_VALUE.toInt(), 
                    when (component) {
                        is JScrollPane -> 150  // JScrollPane的高度
                        else -> 35            // 普通组件的高度
                    }
                )
            }

            // 统一的标签处理
            if (label.isNotEmpty()) {
                val labelComponent = JLabel(label).apply {
                    font = FONT_OPTIONS
                    border = BorderFactory.createEmptyBorder(0, 5, 0, 5)
                    // 移除固定宽度设置，让标签自适应文本长度
                    horizontalAlignment = SwingConstants.RIGHT  // 文本右对齐
                    // 对于JScrollPane，将标签垂直对齐设置为顶部
                    if (component is JScrollPane) {
                        verticalAlignment = JLabel.TOP
                    }
                    
                    // 添加工具提示
                    tooltips[label]?.let { tooltip ->
                        toolTipText = tooltip
                    }
                }
                
                // 创建一个包装面板来容纳标签，并设置最小宽度
                val labelWrapper = JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.X_AXIS)
                    add(Box.createHorizontalGlue())  // 添加弹性空间使标签右对齐
                    add(labelComponent)
                    minimumSize = Dimension(200, 25)  // 设置最小宽度
                    preferredSize = Dimension(200, 25)
                }
                
                itemPanel.add(labelWrapper)
            }

            // 设置组件大小
            when (component) {
                is JTextField -> {
                    component.apply {
                        preferredSize = Dimension(100, 25)
                        maximumSize = preferredSize
                    }
                }
                is JScrollPane -> {
                    component.apply {
                        preferredSize = Dimension(350, 150)
                        maximumSize = preferredSize
                    }
                }
            }

            // 添加组件
            itemPanel.add(Box.createHorizontalStrut(5))  // 添加固定间距
            itemPanel.add(component)
            
            // 不再添加尾部的弹性空间，让组件靠左
            
            panel.add(itemPanel)
            panel.add(Box.createRigidArea(Dimension(0, 5)))
        }
    }


    /**
     * 添加右侧预览面板
     * 用于显示潜在参数信息
     * @param mainPanel 主面板
     */
    private fun addRightPanel(mainPanel: JPanel) {
        val previewPanel = JPanel(BorderLayout(5, 5))
        // 设置固定宽度（30%的总宽度）
        previewPanel.preferredSize = Dimension(240, 0)
        previewPanel.minimumSize = previewPanel.preferredSize
        previewPanel.maximumSize = previewPanel.preferredSize
        
        // 设置边框和标题
        previewPanel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Fuzz Params List:"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        )
        
        // 创建预览文本区域
        val previewArea = JTextArea().apply {
                rows = 10
                columns = 30
                lineWrap = true
                wrapStyleWord = true
                font = FONT_OPTIONS
                text = configs.hiddenParams.joinToString("\n")
                border = BorderFactory.createLineBorder(Color.LIGHT_GRAY)
                // 添加文档监听器
                document.addDocumentListener(object : javax.swing.event.DocumentListener {
                    override fun insertUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()
                    override fun removeUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()
                    override fun changedUpdate(e: javax.swing.event.DocumentEvent) = updateConfig()

                    private fun updateConfig() {
                        configs.hiddenParams.clear()
                        val text = text.trim()
                        if (text.isNotEmpty()) {
                            // 分割并过滤掉空白行
                            val new = text.lines().filter { it.isNotBlank() }
                            configs.hiddenParams.clear()
                            configs.hiddenParams.addAll(new)
                            dataPersistence.updateConfig()
                        }
                    }
                })
        }
        
        // 创建滚动面板
        val scrollPane = JScrollPane(previewArea).apply {
            border = BorderFactory.createEmptyBorder()
        }
        
        // 创建可伸缩的容器面板
        val stretchPanel = JPanel(BorderLayout()).apply {
            add(scrollPane, BorderLayout.CENTER)
            // 添加左右边距
            add(Box.createHorizontalStrut(10), BorderLayout.WEST)
            add(Box.createHorizontalStrut(10), BorderLayout.EAST)
        }
        
        previewPanel.add(stretchPanel, BorderLayout.CENTER)

        // 添加到主面板
        val rightGbc = GridBagConstraints().apply {
            gridx = 1
            gridy = 1
            fill = GridBagConstraints.BOTH
            weightx = 0.3  // 占30%宽度
            weighty = 1.0
            insets = Insets(0, 10, 0, 0)
        }
        mainPanel.add(previewPanel, rightGbc)
    }
}

/**
 *
 */
//fun utils.main() {
//    SwingUtilities.invokeLater {
//        val frame = JFrame("SQL Scout Settings")
//        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
//        frame.setSize(800, 600)
//        frame.isResizable = false    // 禁止调整窗口大小
//
//        val settingPanel = SettingPanel(DataPersistence())
//        frame.contentPane.add(settingPanel)
//        frame.isVisible = true
//    }
//}