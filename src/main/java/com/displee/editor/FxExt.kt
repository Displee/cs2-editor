package com.displee.editor

import com.displee.editor.controller.MainController.Companion.SCRIPTS_INDEX
import javafx.scene.Node
import javafx.scene.control.ButtonType
import javafx.scene.control.ChoiceDialog
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.GridPane
import javafx.scene.layout.RowConstraints

fun notifyChooseScriptId(scriptId: Int): Int? {
    val array = arrayOf("Generate new script id", "Custom script id")
    val dialog = ChoiceDialog(array[0], *array)
    dialog.dialogPane.stylesheets.add("/css/theme.css")
    dialog.dialogPane.stylesheets.add("/css/custom.css")
    dialog.title = "Create new interface"
    dialog.headerText = "Choose your script id"
    dialog.contentText = "Choose:"
    val grid = dialog.dialogPane.children[3] as GridPane
    grid.rowConstraints.add(RowConstraints(45.0))
    grid.add(Label("New script id will be $scriptId."), 1, 1)
    dialog.selectedItemProperty().addListener { obv, oldValue, newValue ->
        if (newValue == array[1]) {
            val field = TextField()
            field.textProperty().addListener { ob, ov, nv ->
                try {
                    val id = field.text.toInt()
                    val cache = mainController.cacheLibrary
                    var exists = cache.data(SCRIPTS_INDEX, id) != null
                    if (id >= 65535) {
                        exists = true
                    }
                    dialog.dialogPane.lookupButton(ButtonType.OK).isDisable = exists
                } catch (e: NumberFormatException) {
                    dialog.dialogPane.lookupButton(ButtonType.OK).isDisable = true
                }
            }
            field.promptText = "Enter script id..."
            grid.add(field, 1, 1)
            dialog.dialogPane.lookupButton(ButtonType.OK).isDisable = true
        } else {
            grid.children.removeIf { node: Node? -> GridPane.getRowIndex(node) == 1 }
            grid.add(Label("New script id will be $scriptId."), 1, 1)
            dialog.dialogPane.lookupButton(ButtonType.OK).isDisable = false
        }
    }
    dialog.dialogPane.minWidth = 400.0
    dialog.dialogPane.minHeight = 250.0
    val result = dialog.showAndWait().orElse(null) ?: return null
    if (result == array[1]) {
        val field = grid.children[3] as TextField
        return field.text.toInt()
    }
    return scriptId
}
