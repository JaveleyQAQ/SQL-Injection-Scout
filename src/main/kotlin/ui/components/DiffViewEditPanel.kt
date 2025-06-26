//package ui.components
//
//import com.github.difflib.text.DiffRow
//import com.github.difflib.text.DiffRowGenerator
//import com.google.gson.GsonBuilder
//import com.google.gson.JsonParser
//import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
//import org.fife.ui.rtextarea.RTextScrollPane
//import java.awt.BorderLayout
//import java.awt.Color
//import javax.swing.JPanel
//
//class DiffViewEditPanel : JPanel() {
//
//    private val maxColumns = 120
//    private val textArea = RSyntaxTextArea(30, 80).apply {
//        isEditable = false
//        highlightSecondaryLanguages= true
//        autoscrolls = true
//        matchedBracketBGColor
//        isCodeFoldingEnabled = true
//        font = font.deriveFont(14f)
//        lineWrap = true
//        wrapStyleWord = true
//        columns = maxColumns
//    }
//    private val rTextScrollPane = RTextScrollPane(textArea)
//
//    init {
////        setSize(1000, 800)
////        setLocationRelativeTo(null)
//        this.layout = BorderLayout()
//        add(rTextScrollPane, BorderLayout.CENTER)
//    }
//
//    private fun formatText(text: String): String {
//        // 处理JSON格式
//        if (text.trim().startsWith("{")) {
//            return try {
//                val gson = GsonBuilder().setPrettyPrinting().create()
//                val json = JsonParser().parse(text)
//                gson.toJson(json)
//            } catch (e: Exception) {
//                text
//            }
//        }
//
//        // 处理HTML格式
//        if (text.contains("<br/>") || text.contains("&quot;") || text.contains("&#") ||
//            text.contains("&lt;")) {
//            return text.replace("<br/>", "\n")
//                .replace("&quot;", "\"")
//                .replace("&#39;", "'")
//                .replace("&lt;", "<")
//                .replace("&gt;", ">")
//                .replace("&amp;", "&")
//        }
//
//        return text
//    }
//
//    fun displayDiff(sections: List<Triple<String, String, String>>) {
//        val displayText = StringBuilder()
//        var lineNumber = 1
//        val highlights = mutableListOf<Pair<Int, Color>>()
//
//        val generator = DiffRowGenerator.create()
//            .showInlineDiffs(false)
//            .mergeOriginalRevised(true)
//            .oldTag { _ -> "" }
//            .newTag { _ -> "" }
//            .columnWidth(maxColumns)
//            .build()
//
//        sections.forEach { (title, oldContent, newContent) ->
//            // 跳过HTTP headers部分
//            if (title.contains("Response Body")) {
//                displayText.appendLine("@@ $title @@")
//                lineNumber++
//
//                // 格式化并比较内容
//                val oldFormatted = formatText(oldContent)
//                val newFormatted = formatText(newContent)
//
//                val rows = generator.generateDiffRows(
//                    oldFormatted.split("\n"),
//                    newFormatted.split("\n")
//                )
//
//                rows.forEach { row ->
//                    when (row.tag) {
//                        DiffRow.Tag.DELETE -> {
//                            displayText.appendLine("-${row.oldLine}")
//                            highlights.add(lineNumber to Color(255, 220, 220))
//                            lineNumber++
//                        }
//
//                        DiffRow.Tag.INSERT -> {
//                            displayText.appendLine("+${row.newLine}")
//                            highlights.add(lineNumber to Color(230, 255, 230))
//                            lineNumber++
//                        }
//
//                        DiffRow.Tag.CHANGE -> {
//                            displayText.appendLine("-${row.oldLine}")
//                            displayText.appendLine("+${row.newLine}")
//                            highlights.add(lineNumber - 1 to Color(255, 220, 220))
//                            highlights.add(lineNumber to Color(230, 255, 230))
//                            lineNumber++
//                            lineNumber++
//                        }
//
//                        else -> {
//                            displayText.appendLine(" ${row.oldLine}")
//                            lineNumber++
//                        }
//                    }
//                }
//            }
//        }
//
//        // 设置文本并添加高亮
//        textArea.text = displayText.toString()
//        textArea.removeAllLineHighlights()
//
//        highlights.forEach { (line, color) ->
//            try {
//                textArea.addLineHighlight(line - 1, color)
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//    }
//
//}
//
