package com.displee.editor.ui.window

import com.displee.editor.controller.AboutController
import com.displee.editor.loadFXML
import com.displee.editor.mainController
import javafx.scene.Scene
import javafx.stage.Modality
import javafx.stage.Stage

class AboutWindow {
	init {
		try {
			val primaryStage = Stage()
			val root = loadFXML("/fxml/about.fxml", AboutController::class.java)
			primaryStage.title = "About"
			primaryStage.scene = Scene(root).also {
				it.stylesheets.add("/css/theme.css")
				it.stylesheets.add("/css/custom.css")
				it.stylesheets.add("/css/highlight.css")
				it.stylesheets.add("/css/code-area-ui.css")
			}
			primaryStage.initModality(Modality.WINDOW_MODAL)
			primaryStage.initOwner(mainController.mainWindow())
			primaryStage.isResizable = false
			primaryStage.show()
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}
}