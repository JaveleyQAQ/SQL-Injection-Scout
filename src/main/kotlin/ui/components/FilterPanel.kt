package ui.components

import java.awt.BorderLayout
import javax.swing.*
import javax.swing.border.EmptyBorder

class FilterPanel : JPanel() {

    init {
        // 设置布局管理器
        layout = BorderLayout()
        // 创建过滤面板
        val label = JLabel("Filter:")
        val textField = JTextField(20)
        val button = JButton("Apply")
        val searchPanel = JPanel()
        searchPanel.layout = BoxLayout(searchPanel, BoxLayout.X_AXIS)
        searchPanel.border = EmptyBorder(1, 3, 1, 3)  //边框
        searchPanel.add(label)
        searchPanel.add(textField)
        searchPanel.add(button)
        // 将搜索面板添加到主面板
        add(searchPanel)

    }

}