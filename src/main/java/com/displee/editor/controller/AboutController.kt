package com.displee.editor.controller

import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.TextArea
import java.net.URL
import java.util.*

class AboutController : Initializable {

	@FXML
	private lateinit var textArea: TextArea

	override fun initialize(location: URL?, resources: ResourceBundle?) {
		textArea.text = """
			Displee's CS2 editor 1.0
			
			
			This application has been created by Displee
			and is powered by https://rscedit.io/.
			Full credits for the CS2 compiler/decompiler goes to Vincent.
			
			
			Copyright (c) 2020, rscedit.
			""".trimIndent()
	}

}