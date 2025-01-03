import com.github.difflib.DiffUtils
import com.github.difflib.text.DiffRow
import com.github.difflib.text.DiffRowGenerator
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rtextarea.RTextScrollPane
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import java.awt.BorderLayout
import java.awt.Color
import java.io.File
import java.nio.file.Files
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import jdk.internal.org.jline.utils.InfoCmp.Capability.columns

class TextEditorDemo : JFrame() {

    private val maxColumns = 120  // 定义最大列宽

    private val textArea = RSyntaxTextArea(30, 80).apply {
        isEditable = false
        isCodeFoldingEnabled = true
        font = font.deriveFont(14f)
        lineWrap = true
        wrapStyleWord = true
        tabSize = 4
        columns = maxColumns
    }

    init {
        title = "GitHub Style Diff Viewer"
        defaultCloseOperation = EXIT_ON_CLOSE

        contentPane = JPanel(BorderLayout()).apply {
            add(RTextScrollPane(textArea))
        }

        setSize(1000, 800)
        setLocationRelativeTo(null)
    }

    private fun formatText(text: String): String {
        // 处理JSON格式
        if (text.trim().startsWith("{")) {
            return try {
                val gson = GsonBuilder().setPrettyPrinting().create()
                val json = JsonParser().parse(text)
                gson.toJson(json)
            } catch (e: Exception) {
                text
            }
        }
        
        // 处理HTML格式
        if (text.contains("<html/>") || text.contains("&quot;") || text.contains("&#")) {
            return text.replace("<br/>", "\n")
                      .replace("&quot;", "\"")
                      .replace("&#39;", "'")
                      .replace("&lt;", "<")
                      .replace("&gt;", ">")
                      .replace("&amp;", "&")
        }
        
        return text
    }

    fun displayDiff(sections: List<Triple<String, String, String>>) {
        val displayText = StringBuilder()
        var lineNumber = 1
        val highlights = mutableListOf<Pair<Int, Color>>()

        val generator = DiffRowGenerator.create()
            .showInlineDiffs(false)
            .mergeOriginalRevised(true)
            .oldTag { "" }
            .newTag { "" }
            .columnWidth(maxColumns)
            .build()

        sections.forEach { (title, oldContent, newContent) ->
            // 跳过HTTP headers部分
            if (title.contains("Response Body")) {
                displayText.appendLine("@@ $title @@")
                lineNumber++

                // 格式化并比较内容
                val oldFormatted = formatText(oldContent)
                val newFormatted = formatText(newContent)
                
                val rows = generator.generateDiffRows(
                    oldFormatted.split("\n"),
                    newFormatted.split("\n")
                )

                rows.forEach { row ->
                    when (row.tag) {
                        DiffRow.Tag.DELETE -> {
                            displayText.appendLine("-${row.oldLine}")
                            highlights.add(lineNumber to Color(255, 220, 220))
                            lineNumber++
                        }
                        DiffRow.Tag.INSERT -> {
                            displayText.appendLine("+${row.newLine}")
                            highlights.add(lineNumber to Color(230, 255, 230))
                            lineNumber++
                        }
                        DiffRow.Tag.CHANGE -> {
                            displayText.appendLine("-${row.oldLine}")
                            displayText.appendLine("+${row.newLine}")
                            highlights.add(lineNumber - 1 to Color(255, 220, 220))
                            highlights.add(lineNumber to Color(230, 255, 230))
                            lineNumber++
                            lineNumber++
                        }
                        else -> {
                            displayText.appendLine(" ${row.oldLine}")
                            lineNumber++
                        }
                    }
                }
            }
        }

        // 设置文本并添加高亮
        textArea.text = displayText.toString()
        textArea.removeAllLineHighlights()
        
        highlights.forEach { (line, color) ->
            try {
                textArea.addLineHighlight(line - 1, color)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SwingUtilities.invokeLater {
                val demo = TextEditorDemo()
                demo.isVisible = true

                val originalText = Files.readAllLines(File("src/original.txt").toPath()).joinToString("\n")
                val revisedText = Files.readAllLines(File("src/new.txt").toPath()).joinToString("\n")
                
                // 创建一个包含Response Body部分的sections列表
                val sections = listOf(
                    Triple("Response Body", originalText, revisedText)
                )
                demo.displayDiff(sections)
            }
        }
    }
}








// 。。。虽然好，但是可能会导致内存泄漏，hacvtor已经移除了，
// 目前商店就JWTEditor在用了


/*
class TextEditorDemo : JFrame() {
    init {
        val cp = JPanel(BorderLayout())


        val textArea = RSyntaxTextArea(20, 60)
        textArea.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_JAVA
        textArea.isCodeFoldingEnabled = true
        val sp = RTextScrollPane(textArea)
        cp.add(sp)

        contentPane = cp
        title = "Text Editor Demo"
        defaultCloseOperation = EXIT_ON_CLOSE
        pack()
        setLocationRelativeTo(null)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // Start all Swing applications on the EDT.
            SwingUtilities.invokeLater {
                TextEditorDemo().isVisible = true
            }


        }
    }

}

*/










// 。。。虽然好看，但是可能会导致内存泄漏，hacvtor已经移除了，
// 目前商店就JWTEditor在用了


/*
class TextEditorDemo : JFrame() {
    init {
        val cp = JPanel(BorderLayout())


        val textArea = RSyntaxTextArea(20, 60)
        textArea.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_JAVA
        textArea.isCodeFoldingEnabled = true
        val sp = RTextScrollPane(textArea)
        cp.add(sp)

        contentPane = cp
        title = "Text Editor Demo"
        defaultCloseOperation = EXIT_ON_CLOSE
        pack()
        setLocationRelativeTo(null)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // Start all Swing applications on the EDT.
            SwingUtilities.invokeLater {
                TextEditorDemo().isVisible = true
            }


        }
    }

}

*/


