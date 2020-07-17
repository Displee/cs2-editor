package com.displee.editor.ui.autocomplete

import com.displee.editor.controller.MainController
import com.displee.editor.ui.autocomplete.item.AutoCompleteClass
import com.displee.editor.ui.autocomplete.item.AutoCompleteFunction
import com.displee.editor.ui.buildStyle
import dawn.cs2.CS2Type
import dawn.cs2.ast.FunctionNode
import javafx.application.Platform
import javafx.geometry.Bounds
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.stage.Popup
import javafx.util.Callback
import org.fxmisc.richtext.CodeArea
import org.reactfx.EventStreams
import org.reactfx.util.Tuple2
import org.reactfx.value.Var
import java.util.*
import java.util.regex.Pattern

class AutoCompletePopup(private val codeArea: CodeArea) : Popup() {

	private val outsideViewportValues = Var.newSimpleVar(false)
	private val list = ListView<AutoCompleteItem>()
	private val pane = BorderPane(list)
	private lateinit var node: FunctionNode

	init {
		setupPane()
		bindCodeAreaEvents()
		bindPopupEvents()
	}

	fun init(node: FunctionNode) {
		this.node = node
	}

	private fun setupPane() {
		content.add(pane)
		pane.prefWidth = 625.0
		pane.styleClass.add("p-0")
		pane.styleClass.add("context-menu-light")
		list.styleClass.add("p-0")
		list.styleClass.add("context-menu-light")
		list.cellFactory = object : Callback<ListView<AutoCompleteItem>, ListCell<AutoCompleteItem>> {
			override fun call(param: ListView<AutoCompleteItem>): ListCell<AutoCompleteItem> {
				return object : ListCell<AutoCompleteItem>() {

					private val name = Label()
					private val type = Label()
					private val hBox = HBox(name, type)

					init {
						type.alignment = Pos.CENTER_RIGHT
						type.maxWidth = Double.MAX_VALUE
						HBox.setHgrow(type, Priority.ALWAYS)
					}

					override fun updateItem(item: AutoCompleteItem?, empty: Boolean) {
						super.updateItem(item, empty)
						if (item == null) {
							graphic = null
							return
						}
						name.text = item.displayName
						type.text = item.returnType.name
						graphic = hBox
					}
				}
			}
		}
	}

	private fun bindCodeAreaEvents() {
		codeArea.textProperty().addListener { _, _, _ ->
			complete()
		}
		//only show popup when inside viewport
		val caretBounds = EventStreams.nonNullValuesOf<Optional<Bounds>>(codeArea.caretBoundsProperty())
		EventStreams.combine(caretBounds, outsideViewportValues.values()).subscribe { tuple3: Tuple2<Optional<Bounds>, Boolean> ->
			val optional = tuple3._1
			if (optional.isPresent) {
				complete()
			} else {
				hide()
			}
		}
	}

	private fun bindPopupEvents() {
		addEventFilter(KeyEvent.KEY_PRESSED) {
			if (!isShowing) {
				return@addEventFilter
			}
			if (it.code == KeyCode.TAB || it.code == KeyCode.ENTER) {
				choose()
			}
		}
		list.setOnMouseClicked {
			if (it.clickCount == 2) {
				choose()
			}
		}
	}

	private fun query(): AutoCompleteQuery? {
		val line = codeArea.getText(codeArea.getAbsolutePosition(codeArea.currentParagraph, 0), codeArea.caretPosition)
		var codeLine = ""
		val codeMatcher = CODE_LINE_PATTERN.matcher(line)
		if (codeMatcher.find()) {
			codeLine = codeMatcher.group()
		}
		if (codeLine.isEmpty()) {
			return null
		}
		var lastWord = ""
		val wordMatcher = LAST_WORD_PATTERN.matcher(codeLine)
		if (wordMatcher.find()) {
			lastWord = wordMatcher.group()
		}
		return AutoCompleteQuery(codeLine, lastWord)
	}

	private fun getObjectScope(line: String): AutoCompleteClass? {
		if (!line.contains(".")) {
			return null
		}
		val split = line.split(".")
		var scope: AutoCompleteClass? = null
		//skip the last one cuz that's what the user is typing
		for(i in 0 until split.size - 1) {
			val variable = split[i]
			var classType = CS2Type.UNKNOWN
			//search vars
			val declared = node.mainScope.getLocalVariableByName(variable)
			if (declared != null) {
				classType = declared.type
			} else {
				//search root items
				val rootItem = AutoCompleteUtils.forItem(variable)
				if (rootItem != null) {
					classType = rootItem.returnType
				}
			}
			if (classType == CS2Type.UNKNOWN) {
				continue
			}
			scope = AutoCompleteUtils.getObject(classType)
			if (scope == null) {
				System.err.println("No object found for class type: $classType")
				continue
			}
		}
		return scope
	}

	private fun complete() {
		val query = query()
		if (query == null) {
			hide()
			return
		}
		val word = query.word
		if (!populate(query) || list.items.isEmpty() || (list.items.size == 1 && list.items.first().name == word)) {
			hide()
			return
		}
		val pointer = codeArea.caretBoundsProperty().value.get()
		show(codeArea, pointer.maxX + OFFSET_X - (word.length * CHAR_WIDTH), pointer.minY + OFFSET_Y)
		Platform.runLater {
			list.refresh()
		}
	}

	private fun populate(query: AutoCompleteQuery): Boolean {
		val word = query.word
		val focusedWord = focusedWord(true)
		if (focusedWord != null && focusedWord.startsWith(word) && focusedWord != word) {
			return false
		}
		val scope = getObjectScope(query.line)
		val showObjectScope = scope != null && query.line.endsWith(".") && word.isEmpty()
		if (!showObjectScope && word.isEmpty()) {
			return false
		}
		list.items.clear()
		val useScopeItems = query.line.substring(0, query.line.length - word.length).endsWith(".") && scope != null
		if (showObjectScope && scope != null) {
			list.items.addAll(scope.children())
		} else if (useScopeItems) {
			list.items.addAll(getQuerySuggestions(word, scope))
		} else {
			list.items.addAll(getQuerySuggestions(word))
		}
		var height = list.items.size * LIST_ITEM_HEIGHT
		if (height > LIST_MAX_HEIGHT) {
			height = LIST_MAX_HEIGHT
		}
		pane.prefHeight = height
		list.prefHeight = pane.prefHeight
		return true
	}

	private fun choose() {
		var item = list.selectionModel.selectedItem
		if (item == null && list.items.isNotEmpty()) {
			item = list.items[0]
		}
		if (item == null) {
			return
		}
		val query = query() ?: return
		var moveCaretBack = false
		var toInsert = item.name.substring(query.word.length)
		if (item is AutoCompleteFunction) {
			toInsert += "()"
			if (item.arguments.isNotEmpty()) {
				moveCaretBack = true
			}
		}
		codeArea.insertText(codeArea.caretPosition, toInsert)
		if (moveCaretBack) {
			codeArea.moveTo(codeArea.caretPosition - 1)
		}
		hide()
	}

	private fun getQuerySuggestions(word: String, scope: AutoCompleteClass? = null): List<AutoCompleteItem> {
		val list = mutableListOf<AutoCompleteItem>()
		if (scope == null) {
			//local variables
			for(i in node.mainScope.copyDeclaredVariables()) {
				if (i.name.startsWith(word)) {
					list.add(AutoCompleteItem(i.name, i.type, AutoCompleteItemType.ARGUMENT))
				}
			}
			//root items
			for (i in AutoCompleteUtils.rootItems()) {
				if (i.name.startsWith(word)) {
					list.add(i)
				}
			}
		} else {
			//scope items
			for(i in scope.children()) {
				if (i.name.startsWith(word)) {
					list.add(i)
				}
			}
		}
		return list
	}

	private fun focusedWord(highlight: Boolean = false): String? {
		val line = codeArea.getText(codeArea.currentParagraph)
		val startPosition = codeArea.getAbsolutePosition(codeArea.currentParagraph, 0)
		val relativeCaretPosition = codeArea.caretPosition - startPosition
		var leftPart = line.substring(0, relativeCaretPosition)
		val rightPart = line.substring(relativeCaretPosition)
		for(i in rightPart) {
			if (!i.isLetterOrDigit()) {
				break
			}
			leftPart += i
		}
		val matcher = LAST_WORD_PATTERN.matcher(leftPart)
		MainController.VAR_LIST.clear()
		var match: String? = null
		if (matcher.find()) {
			match = matcher.group()
			if (highlight) {
				MainController.VAR_LIST.add(match)
			}
		}
		codeArea.buildStyle()
		if (match != null) {
			return match
		}
		return null
	}

	companion object {
		private const val CHAR_WIDTH = 10.8 //TODO Tricky, but works
		private const val LIST_ITEM_HEIGHT = 24.0 //defined in CSS
		private const val LIST_MAX_HEIGHT = 10 * LIST_ITEM_HEIGHT //max 10 items
		private const val OFFSET_X = -15
		private const val OFFSET_Y = 21

		//private val LAST_WORD_PATTERN = Pattern.compile("([a-zA-Z0-9]+)(\\p{Punct}*$)", Pattern.UNICODE_CHARACTER_CLASS)
		private val LAST_WORD_PATTERN = Pattern.compile("([a-zA-Z0-9]+)([^ ~`!@#$%^&*(){}\\[\\];:\"'<,.>?/|_+=-]*$)", Pattern.UNICODE_CHARACTER_CLASS)
		private val CODE_LINE_PATTERN = Pattern.compile("([a-zA-Z0-9.,() _&\\[\\]]+)([\\p{Punct}&&[^.]&&[^(]&&[^)?]&&[^\\[?]&&[^]?]]*$)", Pattern.UNICODE_CHARACTER_CLASS)
	}

}