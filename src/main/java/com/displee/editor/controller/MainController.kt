package com.displee.editor.controller

import javafx.event.ActionEvent
import com.displee.cache.CacheLibrary
import com.displee.cache.ProgressListener
import com.displee.editor.config.ScriptConfiguration
import com.displee.editor.notifyChooseScriptId
import com.displee.editor.ui.alert.Notification
import com.displee.editor.ui.autocomplete.AutoCompleteItem
import com.displee.editor.ui.autocomplete.AutoCompletePopup
import com.displee.editor.ui.autocomplete.AutoCompleteUtils
import com.displee.editor.ui.autocomplete.item.AutoCompleteArgument
import com.displee.editor.ui.autocomplete.item.AutoCompleteFunction
import com.displee.editor.ui.buildStyle
import com.displee.editor.ui.window.AboutWindow
import dawn.cs2.*
import dawn.cs2.CS2Type.*
import dawn.cs2.util.FunctionDatabase
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.BorderPane
import javafx.stage.DirectoryChooser
import javafx.stage.Window
import javafx.util.Callback
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.LineNumberFactory
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.StringBuilder
import java.net.URL
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

class MainController : Initializable {

	@FXML
	private lateinit var openMenuItem: MenuItem

	@FXML
	private lateinit var openRecentPaths: Menu

	@FXML
	private lateinit var path1: MenuItem

	@FXML
	private lateinit var path2: MenuItem

	@FXML
	private lateinit var path3: MenuItem

	@FXML
	private lateinit var saveMenuItem: MenuItem

	@FXML
	private lateinit var buildMenuItem: MenuItem

	@FXML
	private lateinit var showAssemblyMenuItem: CheckMenuItem

	@FXML
	private lateinit var newMenuItem: MenuItem

	@FXML
	private lateinit var aboutMenuItem: MenuItem

	@FXML
	private lateinit var searchField: TextField

	@FXML
	private lateinit var rootPane: BorderPane

	@FXML
	private lateinit var statusLabel: Label

	@FXML
	private lateinit var scriptList: ListView<Int>

	@FXML
	private lateinit var mainCodePane: BorderPane

	@FXML
	private lateinit var tabPane: TabPane

	@FXML
	private lateinit var compilePane: BorderPane

	@FXML
	private lateinit var compileArea: TextArea

	@FXML
	private lateinit var assemblyCodePane: BorderPane

	private val cachedScripts = mutableMapOf<Int, String>()

	private final val fileName = System.getProperty("user.dir") + "/paths.txt"
	private var temporaryAssemblyPane: Node? = null

	lateinit var cacheLibrary: CacheLibrary
	private lateinit var scripts: IntArray

	private lateinit var scriptConfiguration: ScriptConfiguration
	private lateinit var opcodesDatabase: FunctionDatabase
	private lateinit var scriptsDatabase: FunctionDatabase

	private var currentScript: CS2? = null

	override fun initialize(location: URL?, resources: ResourceBundle?) {
		var file = File(fileName)
		if (!file.exists()) file.createNewFile()
		else {
			var readLines = file.readLines()
			if (readLines != null && !readLines.isEmpty()) {
				path1.text = readLines.get(0)
				if (readLines.size > 1) {
					path2.text = readLines.get(1)
				}
				if (readLines.size > 2) {
					path3.text = readLines.get(2)
				}
			}
		}

		rootPane.addEventHandler(KeyEvent.KEY_PRESSED) { e: KeyEvent ->
			if (e.isControlDown && e.code == KeyCode.N) {
				if (!this::cacheLibrary.isInitialized) {
					return@addEventHandler
				}
				newScript(notifyChooseScriptId(cacheLibrary.index(SCRIPTS_INDEX).nextId()))
			}
		}

		openMenuItem.setOnAction {
			openCache()
		}

		path1.setOnAction { openCacheFromRecentPaths(1) }

		path2.setOnAction { openCacheFromRecentPaths(2) }

		path3.setOnAction { openCacheFromRecentPaths(3) }

		saveMenuItem.setOnAction {
			compileScript()
		}
		buildMenuItem.setOnAction {
			packScript()
		}
		showAssemblyMenuItem.setOnAction {
			if (showAssemblyMenuItem.isSelected) {
				rootPane.right = temporaryAssemblyPane
				temporaryAssemblyPane = null
			} else {
				temporaryAssemblyPane = rootPane.right
				rootPane.right = null
			}
		}
		newMenuItem.setOnAction {
			newScript(notifyChooseScriptId(cacheLibrary.index(SCRIPTS_INDEX).nextId()))
		}
		aboutMenuItem.setOnAction {
			AboutWindow()
		}
		scriptList.cellFactory = object : Callback<ListView<Int>, ListCell<Int>> {
			override fun call(param: ListView<Int>): ListCell<Int> {
				return object : ListCell<Int>() {
					override fun updateItem(item: Int?, empty: Boolean) {
						super.updateItem(item, empty)
						if (item == null) {
							return
						}
						super.setText("Script $item")
					}
				}
			}
		}
		scriptList.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue ->
			if (newValue == null) {
				return@addListener
			}
			status("decompiling script $newValue")
			currentScript = readScript(newValue)
			if (currentScript == null) {
				//TODO Popup failed to read script
				status("ready")
				return@addListener
			}
			val hash = cacheLibrary.hashCode().toString() + " - " + newValue.toString().hashCode()
			for(tab in tabPane.tabs) {
				if (tab.properties["hash"] == hash) {
					tabPane.selectionModel.select(tab)
					return@addListener
				}
			}
			val script = decompileScript()
			status("ready")
			val codeArea = createCodeArea(script, true)
			val tab = Tab("Script $newValue", BorderPane(VirtualizedScrollPane(codeArea)))
			tab.properties["hash"] = hash
			tab.userData = currentScript
			tabPane.tabs.add(tab)
			tabPane.selectionModel.selectLast()
			refreshAssemblyCode()
			compileScript()
		}
		searchField.textProperty().addListener { observable, oldValue, newValue ->
			if (newValue == null) {
				return@addListener
			}
			search(newValue)
		}
		tabPane.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue ->
			if (oldValue != null) {
				val codeArea = ((oldValue.content as BorderPane).center as VirtualizedScrollPane<MainCodeArea>).content as MainCodeArea
				codeArea.autoCompletePopup?.hide()
			}
			if (newValue == null || newValue.userData == null) {
				if (newValue == null) {
					replaceAssemblyCode("")
				}
				return@addListener
			}
			currentScript = newValue.userData as CS2
			refreshAssemblyCode()
		}
		assemblyCodePane.center = BorderPane(VirtualizedScrollPane(createCodeArea("", editable = false)))

		//Disable assembly pane by default
		showAssemblyMenuItem.selectedProperty().set(false)
		showAssemblyMenuItem.onAction.handle(null)

		//init singleton
		AutoCompleteUtils
	}

	private fun openCacheFromRecentPaths(lineNumber: Int) {
		val fileName = System.getProperty("user.dir") + "/paths.txt"
		var file = File(fileName)
		if (file.exists()) {
			if (file.readLines() == null || file.readLines().isEmpty()) {
				Platform.runLater {
					Notification.error("No recent paths saved, open a new cache")
					clearCache()
				}
			} else {
				openCache(File(file.readLines()[lineNumber-1]))
			}
		}
	}

	private fun shiftLines() {
		if (path1.text.isEmpty()) return;
		path3.text = path2.text
		path2.text = path1.text
	}

	private fun savePaths() {
		var file = File(fileName)
		if (!file.exists()) file.createNewFile()
		val string = StringBuilder()
		string.append(path1.text).append("\n").append(path2.text).append("\n").append(path3.text)

		file.writeText(string.toString())
	}

	private fun openCache(f: File? = null) {
		var mehFile = f
		if (mehFile == null) {
			val chooser = DirectoryChooser()
			mehFile = chooser.showDialog(mainWindow()) ?: return
			shiftLines()
			path1.text = mehFile!!.path
		}
		savePaths()
//		val string = StringBuilder()
//		if (file.readLines() != null && file.readLines().isNotEmpty()) {
//			for ((index, readLine) in file.readLines().withIndex()) {
//				string.append(readLine)
//				if (index == 2) break;
//				string.append("\n")
//			}
//			file.writeText(string.toString())
//		}


		scriptList.isDisable = true
		GlobalScope.launch {
			try {
				cacheLibrary = CacheLibrary(mehFile.absolutePath, listener = object : ProgressListener {
					override fun notify(progress: Double, message: String?) {
						if (message == null) {
							return
						}
					}
				})
				if (!cacheLibrary.exists(SCRIPTS_INDEX)) {
					Platform.runLater {
						Notification.error("Can't find any scripts in the cache you trying to load.")
						clearCache()
					}
					return@launch
				} else if (cacheLibrary.isRS3()) {
					Platform.runLater {
						Notification.error("RS3 caches are not supported.")
						clearCache()
					}
				}
				loadScripts()
				createScriptConfigurations()
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	private fun loadScripts() {
		status("Populating scripts...")
		val ids = cacheLibrary.index(SCRIPTS_INDEX).archiveIds()
		scripts = ids.copyOf()
		search(searchField.text)
	}

	private fun search(text: String) {
		val list = arrayListOf<Int>()
		for(i in scripts) {
			try {
				if (text.startsWith("op_")) {
					val data = cacheLibrary.data(SCRIPTS_INDEX, i)
					val script = CS2Reader.readCS2ScriptNewFormat(data, i, scriptConfiguration.unscrambled, scriptConfiguration.disableSwitches, scriptConfiguration.disableLongs)
					val opcode = text.replace("op_", "").toInt()
					for (instruction in script.instructions) {
						if (instruction.opcode == opcode) {
							list.add(i)
							break
						}
					}
				} else if (text.isNotEmpty() && !i.toString().startsWith(text)) {
					val cached = cachedScripts[i]
					if (::scriptConfiguration.isInitialized && cached != null && cached.contains(text)) {
						list.add(i)
					}
				} else {
					list.add(i)
				}
			} catch(t: NumberFormatException) {
				list.add(i)
			}
		}
		scriptList.items.clear()
		scriptList.items.addAll(list)
	}

	private fun createCodeArea(initialText: String, showLineNumbers: Boolean = false, editable: Boolean = true): MainCodeArea {
		val codeArea = MainCodeArea(editable)
		if (showLineNumbers) {
			codeArea.paragraphGraphicFactory = LineNumberFactory.get(codeArea)
		}
		codeArea.buildStyle()
		val whiteSpace = Pattern.compile("^\\s+")
		codeArea.addEventHandler(KeyEvent.KEY_PRESSED) { e: KeyEvent ->
			if (e.isControlDown && e.code == KeyCode.S) {
				compileScript()
			} else if (e.isControlDown && e.code == KeyCode.D) {
				packScript()
			} else if (e.code == KeyCode.ENTER) {
				val caretPosition = codeArea.caretPosition
				val currentParagraph = codeArea.currentParagraph
				val m0 = whiteSpace.matcher(codeArea.getParagraph(currentParagraph - 1).segments[0])
				if (m0.find()) {
					Platform.runLater { codeArea.insertText(caretPosition, m0.group()) }
				}
			} else if (e.isShiftDown && e.code == KeyCode.TAB) {
				if (codeArea.text.substring(codeArea.caretPosition - 1, codeArea.caretPosition) == "\t") {
					codeArea.deletePreviousChar()
				}
			}
		}
		codeArea.isEditable = editable
		codeArea.replaceText(0, 0, initialText)
		return codeArea
	}

	private fun createScriptConfigurations() {
		status("guessing script configuration...")
		val scriptConfiguration = guessConfiguration()
		if (scriptConfiguration == null) {
			Platform.runLater {
				Notification.error("Unable to find correct script configuration for this cache.")
			}
			clearCache()
			return
		}
		println("Using config: $scriptConfiguration")
		this.scriptConfiguration = scriptConfiguration
		opcodesDatabase = FunctionDatabase(javaClass.getResourceAsStream(scriptConfiguration.opcodeDatabase), false, scriptConfiguration.scrambled)
		status("generating script signatures...")
		scriptsDatabase = generateScriptsDatabase(scriptConfiguration)
		status("generating auto complete items...")
		generateAutoCompleteItems()
		status("caching all scripts...")
		cacheAllScripts()
		status("ready")
		Platform.runLater {
			scriptList.isDisable = false
			newMenuItem.isDisable = false
			saveMenuItem.isDisable = false
			buildMenuItem.isDisable = false
		}
	}

	private fun guessConfiguration(): ScriptConfiguration? {
		val testUnit = { config: ScriptConfiguration -> Boolean
			val ids = cacheLibrary.index(SCRIPTS_INDEX).archiveIds()
			var error = 0
			for(id in ids) {
				val data = cacheLibrary.data(SCRIPTS_INDEX, id)
				try {
					CS2Reader.readCS2ScriptNewFormat(data, id, config.unscrambled, config.disableSwitches, config.disableLongs)
				} catch(e: Throwable) {
					if (config.version == 179) {
						println(e)
						println("id $id")

					}
//					error++
//					if (error >= 2) {
//						break
//					}
				}
			}
//			println("config: ${config.version} $error")
			error < 2
		}
		var configuration: ScriptConfiguration? = null
		if (cacheLibrary.isOSRS()) {
			val configurations = arrayOf(
				ScriptConfiguration(154, "/cs2/opcode/database/osrs.ini", false, true),
				ScriptConfiguration(176, "/cs2/opcode/database/osrs.ini", false, true),
				ScriptConfiguration(179, "/cs2/opcode/database/osrs.ini", false, true)
			)
			for (i in configurations) {
				if (testUnit(i)) {
					configuration = i
				}
			}
		} else {
			val configurations = arrayOf(
					//< 500
					ScriptConfiguration(464, "/cs2/opcode/database/rs2_new.ini", true, true),
					//>= 500 && < 643
					ScriptConfiguration(667, "/cs2/opcode/database/rs2_old.ini", false, true),
					//>= 643
					ScriptConfiguration(667, "/cs2/opcode/database/rs2_new.ini", false, false)
					//718
					//ScriptConfiguration(718, "/cs2/opcode/database/rs2_new.ini", false, false) //TODO Fix 718
			)
			for (i in configurations) {
				if (testUnit(i)) {
					configuration = i
				}
			}
		}
		return configuration
	}

	private fun generateScriptsDatabase(configuration: ScriptConfiguration, loop: Int = 6): FunctionDatabase {
		val opcodesDatabase = FunctionDatabase(javaClass.getResourceAsStream(configuration.opcodeDatabase), false, configuration.scrambled)
		val scriptsDatabase = FunctionDatabase()
		val ids = cacheLibrary.index(SCRIPTS_INDEX).archiveIds()
		for (l in 0 until loop) {
			for (id in ids) {
				val data = cacheLibrary.data(SCRIPTS_INDEX, id)
				try {
					val script = CS2Reader.readCS2ScriptNewFormat(data, id, configuration.unscrambled, configuration.disableSwitches, configuration.disableLongs)
					val decompiler = CS2Decompiler(script, opcodesDatabase, scriptsDatabase)
					try {
						decompiler.decompile()
					} catch (ex: Throwable) {

					}
					val function = decompiler.function
					if (function.returnType == UNKNOWN) {
						continue
					}
					val info = scriptsDatabase.getInfo(id)
					info.name = function.name
					if (info.returnType === UNKNOWN) {
						info.returnType = function.returnType
					}
					for (a in function.argumentLocals.indices) {
						info.argumentTypes[a] = function.argumentLocals[a].type
						info.argumentNames[a] = function.argumentLocals[a].name
					}
				} catch (e: Exception) {
					e.printStackTrace() //unknown scrambling etc
				}
			}
		}
		var successCount = 0
		val writer = StringWriter()
		for (id in ids) {
			val info = scriptsDatabase.getInfo(id)
			if (info?.getReturnType() == null /* || info.getReturnType() == CS2Type.UNKNOWN*/) {
				continue
			}
			if (info.returnType != UNKNOWN) {
				successCount++
			}
			if (info.getReturnType().isStructure) {
				writer.write("$id ${info.getName()} {${info.getReturnType().toString().replace(" ".toRegex(), "")}}")
			} else {
				writer.write("$id ${info.getName()} ${info.getReturnType()}")
			}
			for (a in info.getArgumentTypes().indices) {
				writer.write(" ${info.getArgumentTypes()[a]} ${info.getArgumentNames()[a]}")
			}
			writer.write("\r\n")
		}
		println("Generated $successCount/${ids.size} script signatures.")
		val signatures = writer.toString()
		return FunctionDatabase(signatures, true, null)
	}

	private fun generateAutoCompleteItems() {
		AutoCompleteUtils.dynamicItems.clear()
		AutoCompleteUtils.clearDynamicChildren()
		val text = javaClass.getResource(scriptConfiguration.opcodeDatabase).readText()
		for(line in text.lines()) {
			if (line.isEmpty() || line.startsWith(" ") || line.startsWith("//") || line.startsWith("#")) {
				continue
			}
			val split = line.split(" ")
			val opcode = split[0].toInt()
			if (!scriptConfiguration.scrambled.containsKey(opcode)) {
				continue
			}
			var list: MutableList<AutoCompleteItem>? = AutoCompleteUtils.dynamicItems
			if (FlowBlocksGenerator.isObjectOpcode(opcode) || FlowBlocksGenerator.isObjectWidgetOpcode(opcode)) {
				list = AutoCompleteUtils.getObject(WIDGET_PTR, true)?.dynamicChildren
			}
			val name = split[1]
			val returnTypes = if (split[2].contains("|")) {
				val multiReturn = split[2].split("\\|".toRegex())
				Array(multiReturn.size) {
					forDesc(multiReturn[it])
				}
			} else {
				arrayOf(forDesc(split[2]))
			}
			val argSize = (split.size - 2) / 2
			val argTypes = Array(argSize) {
				val index = 3 + (it * 2)
				forDesc(split[index])
			}
			val argNames = Array(argSize) {
				val index = 3 + (it * 2)
				split[index + 1]
			}
			val function = AutoCompleteFunction(name, returnTypes[0], Array(argSize) {
				AutoCompleteArgument(argNames[it], argTypes[it])
			})
			if (list != null && list.firstOrNull { it.name == name } == null) {
				list.add(function)
			}
		}
	}

	private fun readScript(id: Int): CS2? {
		val data = cacheLibrary.data(SCRIPTS_INDEX, id)
		return CS2Reader.readCS2ScriptNewFormat(data, id, scriptConfiguration.unscrambled, scriptConfiguration.disableSwitches, scriptConfiguration.disableLongs)
	}

	private fun cacheAllScripts() {
		if (!::scriptConfiguration.isInitialized) {
			return
		}
		cachedScripts.clear()
		for (i in scripts) {
			try {
				val decompiled = decompileScript(i)
				cachedScripts[i] = decompiled
			} catch (e: java.lang.Exception) {

			}
		}
	}

	private fun decompileScript(): String {
		val script = currentScript ?: return ""
		val decompiler = CS2Decompiler(script, opcodesDatabase, scriptsDatabase)
		try {
			decompiler.decompile()
		} catch(t: Throwable) {
			t.printStackTrace()
		}
		decompiler.optimize()
		val printer = CodePrinter()
		decompiler.function.print(printer)
		return printer.toString()
	}

	private fun decompileScript(id: Int): String {
		val data = cacheLibrary.data(SCRIPTS_INDEX, id)
		val script = CS2Reader.readCS2ScriptNewFormat(data, id, scriptConfiguration.unscrambled, scriptConfiguration.disableSwitches, scriptConfiguration.disableLongs)
		val decompiler = CS2Decompiler(script, opcodesDatabase, scriptsDatabase)
		try {
			decompiler.decompile()
		} catch(t: Throwable) {
			//t.printStackTrace()
		}
		decompiler.optimize()
		val printer = CodePrinter()
		decompiler.function.print(printer)
		return printer.toString()
	}

	private fun compileScript() {
		val script = currentScript ?: return
		val activeCodeArea = activeCodeArea()
		try {
			val parser = CS2ScriptParser.parse(activeCodeArea.text, opcodesDatabase, scriptsDatabase)
			activeCodeArea.autoCompletePopup?.init(parser)
			refreshAssemblyCode()
			printConsoleMessage("Compiled script ${script.scriptID}.")
		} catch(t: Throwable) {
			t.printStackTrace()
			printConsoleMessage(t.message)
		}
	}

	private fun newScript(newId: Int?) {
		if (newId == null) {
			return
		}
		val function = CS2ScriptParser.parse("void script_$newId() {\n\treturn;\n}", opcodesDatabase, scriptsDatabase)
		val compiler = CS2Compiler(function, scriptConfiguration.scrambled, scriptConfiguration.disableSwitches, scriptConfiguration.disableLongs)
		val compiled = compiler.compile(null) ?: throw Error("Failed to compile.")
		cacheLibrary.put(SCRIPTS_INDEX, newId, compiled)

		if (!cacheLibrary.index(SCRIPTS_INDEX).update()) {
			Notification.error("Failed to create new script with id $newId.")
		} else {
			Notification.info("A new script has been created with id $newId.")
			loadScripts()
		}
	}

	private fun packScript() {
		val script = currentScript ?: return
		val activeCodeArea = activeCodeArea()
		try {
			val function = CS2ScriptParser.parse(activeCodeArea.text, opcodesDatabase, scriptsDatabase)
			val compiler = CS2Compiler(function, scriptConfiguration.scrambled, scriptConfiguration.disableSwitches, scriptConfiguration.disableLongs)
			val compiled = compiler.compile(null) ?: throw Error("Failed to compile.")
			cacheLibrary.put(SCRIPTS_INDEX, script.scriptID, compiled)
			activeCodeArea.autoCompletePopup?.init(function)
			if (cacheLibrary.index(SCRIPTS_INDEX).update()) {
				printConsoleMessage("Packed script ${script.scriptID} successfully.")
			} else {
				printConsoleMessage("Failed to pack script ${script.scriptID}.")
			}
		} catch(t: Throwable) {
			t.printStackTrace()
			printConsoleMessage(t.message)
		}
	}

	private fun printConsoleMessage(line: String?) {
		compileArea.text = timeFormat.format(Date.from(Instant.now())) + " -> " + line + System.lineSeparator() + compileArea.text
	}

	private fun refreshAssemblyCode() {
		try {
			val parser = CS2ScriptParser.parse(activeCodeArea().text, opcodesDatabase, scriptsDatabase)
			val compiler = CS2Compiler(parser, scriptConfiguration.scrambled, scriptConfiguration.disableSwitches, scriptConfiguration.disableLongs)
			val stringWriter = StringWriter()
			val writer = PrintWriter(stringWriter)
			compiler.compile(writer)
			replaceAssemblyCode(stringWriter.toString())
		} catch(e: Exception) {
			//do nothing
			//replaceAssemblyCode("Failed to generate assembly code.")
		}
	}

	private fun replaceAssemblyCode(code: String) {
		val assemblyCodeArea = ((assemblyCodePane.center as BorderPane).center as VirtualizedScrollPane<CodeArea>).content as CodeArea
		assemblyCodeArea.replaceText(0, assemblyCodeArea.length, code)
	}

	private fun clearCache() {
		tabPane.tabs.clear()
		scriptList.items.clear()
		scriptList.isDisable = true
		newMenuItem.isDisable = true
		saveMenuItem.isDisable = true
		buildMenuItem.isDisable = true
	}

	private fun status(status: String) {
		Platform.runLater {
			statusLabel.text = "Status: $status"
		}
	}

	private fun activeCodeArea(): MainCodeArea {
		return ((tabPane.selectionModel.selectedItem.content as BorderPane).center as VirtualizedScrollPane<CodeArea>).content as MainCodeArea
	}

	fun mainWindow(): Window {
		return mainCodePane.scene.window
	}

	companion object {

		const val SCRIPTS_INDEX = 12

		var timeFormat = SimpleDateFormat("HH:mm:ss")
		val VAR_LIST = mutableListOf<String>()
		val KEYWORDS = arrayOf("string", "string[]", "boolean", "break", "case", "char", "continue", "default", "do", "else", "for", "goto", "if", "int", "int[]", "long", "long[]", "return", "switch", "this", "void", "while", "true", "false", "null")
		private val BI_CLASSES = arrayOf(
				FONTMETRICS,
				SPRITE,
				MODEL,
				MIDI,
				DATAMAP,
				ATTRIBUTEMAP,
				CONTAINER,
				WIDGET_PTR,
				LOCATION,
				ITEM,
				COLOR,
				IDENTIKIT,
				ANIM,
				MAPID,
				GRAPHIC,
				SKILL,
				NPCDEF,
				QCPHRASE,
				CHATCAT,
				TEXTURE,
				STANCE,
				SPELL,
				CATEGORY,
				SOUNDEFFECT,
				CALLBACK
		)

		private val KEYWORD_PATTERN = "\\b(" + java.lang.String.join("|", *KEYWORDS.map { it.replace("[", "\\[").replace("]", "\\]") }.toTypedArray()) + ")\\b"
		private var VAR_PATTERN = "\\b(" + java.lang.String.join("|", *VAR_LIST.toTypedArray()) + ")\\b"
		private val BICLASS_PATTERN = "\\b(" + java.lang.String.join("|", *BI_CLASSES.map { it.name.replace("[", "\\[").replace("]", "\\]") }.toTypedArray()) + ")\\b"

		private const val PAREN_PATTERN = "\\(|\\)"
		private const val BRACE_PATTERN = "\\{|\\}"
		private const val BRACKET_PATTERN = "\\[|\\]"
		private const val SEMICOLON_PATTERN = "\\;"
		private const val STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\""
		private const val NUMBER_PATTERN = "\\b\\d+\\b"
		private const val COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/"
		private const val COLOR_PATTERN = "0x.{3,6}\\b"
		//TODO Fix these highlightings
		private val CS2_CALL_PATTERN = "\\bscript_\\d+(.*)\\b"
		private val CS2_HOOK_PATTERN = "\\b&script_\\d+(.*)"

		fun computeHighlighting(text: String): StyleSpans<Collection<String>>? {
			VAR_PATTERN = "\\b(" + java.lang.String.join("|", *VAR_LIST.toTypedArray()) + ")\\b"
			val pattern = Pattern.compile("(?<KEYWORD>$KEYWORD_PATTERN)|(?<BICLASS>$BICLASS_PATTERN)|(?<NUMBER>$NUMBER_PATTERN)|(?<PAREN>$PAREN_PATTERN)|(?<BRACE>$BRACE_PATTERN)|(?<BRACKET>$BRACKET_PATTERN)|(?<SEMICOLON>$SEMICOLON_PATTERN)|(?<STRING>$STRING_PATTERN)|(?<COMMENT>$COMMENT_PATTERN)|(?<COLOR>$COLOR_PATTERN)|(?<VAR>$VAR_PATTERN)"/*|(?<CS2CALL>$CS2_CALL_PATTERN)|(?<CS2HOOK>$CS2_HOOK_PATTERN)"*/)
			val matcher: Matcher = pattern.matcher(text)
			var lastKwEnd = 0
			val spansBuilder = StyleSpansBuilder<Collection<String>>()
			while (matcher.find()) {
				val styleClass = when {
					matcher.group("KEYWORD") != null -> "keyword"
					matcher.group("PAREN") != null -> "paren"
					matcher.group("BRACE") != null -> "brace"
					matcher.group("BRACKET") != null -> "bracket"
					matcher.group("SEMICOLON") != null -> "semicolon"
					matcher.group("STRING") != null -> "string"
					matcher.group("NUMBER") != null -> "number"
					matcher.group("COMMENT") != null -> "comment"
					matcher.group("BICLASS") != null -> "biclass"
					/*matcher.group("CS2CALL") != null -> "cs2-call"
					matcher.group("CS2HOOK") != null -> "cs2-hook"*/
					matcher.group("COLOR") != null -> "color"
					matcher.group("VAR") != null -> "var"
					else -> null
				}
				if (styleClass == null) {
					continue
				}
				spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd)
				spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start())
				lastKwEnd = matcher.end()
			}
			spansBuilder.add(Collections.emptyList(), text.length - lastKwEnd)
			return spansBuilder.create()
		}

	}

	private class MainCodeArea(autoComplete: Boolean = true) : CodeArea() {
		val autoCompletePopup: AutoCompletePopup? = if (autoComplete) AutoCompletePopup(this) else null
	}

}
