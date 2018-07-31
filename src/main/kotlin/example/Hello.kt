package example

import javafx.geometry.Insets
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import tornadofx.*
import java.io.File
import java.nio.charset.Charset

private val singleDictFile = File("data/单字码表.txt")

private val singleWordDict: List<Word> = singleDictFile.readLines().map {
    val (name, code) = it.split("\t")
    Word(name, code)
}

private val userHome = System.getProperty("user.home")

private val mainDict: List<Word> = readLuogeDict(File(userHome, "Documents/落格输入法/小-鹤-主-码-表(BIG).txt"))

private val customDictFile = File(userHome, "Documents/落格输入法/new.txt")

private var customDict: List<Word> = readLuogeDict(customDictFile)

private fun readLuogeDict(file: File): List<Word> {
    return file.readLines(Charset.forName("UTF-16")).map {
        val (name, code, index) = it.split("\t")
        Word(name, code, index.toInt())
    }
}

private fun reloadCustomDict() {
    customDict = readLuogeDict(customDictFile)
}

data class Word(val name: String, val code: String, var index: Int? = null) {
    override fun toString(): String {
        return "$name\t$code" + (index?.let { "\t$it" } ?: "")
    }
}

class HelloWorld : View() {
    private var wordField: TextField by singleAssign()
    override val root = vbox {
        hbox {
            textfield {
                HBox.setHgrow(this, Priority.ALWAYS)
                wordField = this
            }
            button("Add").setOnAction { _ ->
                val word = wordField.text.trim()
                if (word.isNotEmpty()) {
                    val existing = fetchExisting(word)
                    if (existing == null) {
                        addToCustomDict(word)
                    } else {
                        information("Operation Failed", "The word $word is already in working dict")
                    }
                }
            }
            button("Delete").setOnAction { _ ->
                val word = wordField.text.trim()
                if (word.isNotEmpty()) {
                    fetchExistingInCustomDict(word)?.let { existing ->
                        deleteCustomWord(existing)
                    } ?: information("Operation Failed", "The word $word is not found in custom dict")
                }
            }
            spacing = 10.0
        }
        label {
            addClass(HelloWorldStyle.code)
            wordField.textProperty().addListener { _, _, newValue ->
                this.text = calcCodeFor(newValue?.trim())
            }
        }
        vbox {
            label("词库中查找：")
            label {
                wordField.textProperty().addListener { _, _, newValue ->
                    this.text = fetchExisting(newValue)?.toString() ?: "不存在"
                }
            }
        }
        spacing = 10.0
        padding = Insets(10.0)
    }

}

private fun addToCustomDict(word: String) {
    val code = calcCodeFor(word)
    val newWords = customDict + Word(name = word, code = code, index = calcNewIndexForCode(code))
    writeToCustomDictFile(newWords)
    information("添加成功", "成功添加了词语 $word，在输入法中自定义编码处删除所有编码并重新导入才能生效！")
    reloadCustomDict()
}

fun calcNewIndexForCode(code: String): Int {
    return (mainDict + customDict).filter { it.code == code }.map { it.index!! }.max()?.let { it + 1 } ?: 1
}

private fun deleteCustomWord(word: Word) {
    writeToCustomDictFile(customDict.filterNot { it == word })
    information("删除成功", "成功删除了词语 $word，在输入法中自定义编码处删除所有编码并重新导入才能生效！")
    reloadCustomDict()
}

private fun writeToCustomDictFile(lines: List<Word>) {
    customDictFile.writeText(
            lines.joinToString("\n") { it.toString() },
            Charset.forName("UTF-16")
    )
}

private fun fetchExisting(text: String?): Word? {
    return fetchExistingInMainDict(text) ?: fetchExistingInCustomDict(text)
}

private fun fetchExistingInMainDict(text: String?): Word? {
    if (text == null || text.isEmpty()) return null
    return (mainDict + customDict).find { it.name == text }
}


private fun fetchExistingInCustomDict(text: String?): Word? {
    if (text == null || text.isEmpty()) return null
    return (mainDict + customDict).find { it.name == text }
}


private fun calcCodeFor(text: String?): String {
    if (text == null || text.isEmpty()) return ""
    fun findCharCode(char: Char, useSize: Int): String = findChar(char)?.code?.take(useSize) ?: " error($char) "
    return when (text.length) {
        1 -> findCharCode(text.first(), 2)
        2 -> text.map { findCharCode(it, 2) }.joinToString("")
        3 -> text.take(2).map { findCharCode(it, 1) }.joinToString("") + findCharCode(text.last(), 2)
        else -> text.take(3).map { findCharCode(it, 1) }.joinToString("") + findCharCode(text.last(), 1)
    }
}

private fun findChar(char: Char): Word? {
    return singleWordDict.find { it.name == char.toString() }
}

class HelloWorldStyle : Stylesheet() {
    companion object {
        val code by cssclass()
    }

    init {
        root {
            prefWidth = 400.px
            prefHeight = 400.px
        }
        code {
            fontSize = 26.px
        }
    }
}

class HelloWorldApp : App(HelloWorld::class, HelloWorldStyle::class)

fun main(args: Array<String>) {
    launch<HelloWorldApp>()
}