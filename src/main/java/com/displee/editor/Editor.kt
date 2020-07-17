package com.displee.editor

import com.displee.editor.controller.MainController
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage

lateinit var mainController: MainController

fun main(vararg args: String) {
	Application.launch(Editor::class.java, *args)
}

fun loadFXML(resource: String, controllerClass: Class<out Initializable>): Parent {
	val loader = FXMLLoader(Editor::class.java.getResource(resource))
	loader.setController(controllerClass.newInstance())
	return loader.load()
}

class Editor : Application() {

	override fun start(stage: Stage) {
		mainController = MainController()
		val loader = FXMLLoader(Editor::class.java.getResource("/fxml/main.fxml"))
		loader.setController(mainController)
		val root = loader.load<Parent>()
		stage.scene = Scene(root, 1200.0, 800.0).also {
			it.stylesheets.add("/css/theme.css")
			it.stylesheets.add("/css/custom.css")
			it.stylesheets.add("/css/highlight.css")
			it.stylesheets.add("/css/code-area-ui.css")
		}
		stage.title = "Displee's CS2 editor"
		stage.show()
	}

}