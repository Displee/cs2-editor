package com.displee.editor.ui.alert

import javafx.scene.control.Alert

class Notification {

	private val title: String
	private val header: String
	private val content: String
	private val type: Alert.AlertType

	private constructor(title: String, header: String = "", content: String = "", type: Alert.AlertType) {
		this.title = title
		this.header = header
		this.content = content
		this.type = type
	}

	companion object {
		fun info(content: String = "", header: String? = null, wait: Boolean = false) {
			val alert = Alert(Alert.AlertType.INFORMATION, content)
			alert.title = "Information"
			alert.headerText = header
			if (header == null) {
				alert.graphic = null
			}
			if (wait) {
				alert.showAndWait()
			} else {
				alert.show()
			}
		}

		fun error(content: String = "", header: String? = null, wait: Boolean = false) {
			val alert = Alert(Alert.AlertType.ERROR, content)
			alert.title = "Error"
			alert.headerText = header
			if (header == null) {
				alert.graphic = null
			}
			if (wait) {
				alert.showAndWait()
			} else {
				alert.show()
			}
		}
	}

}