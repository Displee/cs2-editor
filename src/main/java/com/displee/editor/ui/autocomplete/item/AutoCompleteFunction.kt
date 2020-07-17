package com.displee.editor.ui.autocomplete.item

import com.displee.editor.ui.autocomplete.AutoCompleteItem
import com.displee.editor.ui.autocomplete.AutoCompleteItemType
import dawn.cs2.CS2Type

class AutoCompleteFunction(name: String, returnType: CS2Type, val arguments: Array<AutoCompleteArgument>)
	: AutoCompleteItem(name, returnType, AutoCompleteItemType.METHOD) {

	override var displayName = "$name(${arguments.joinToString(", ") { "${it.returnType.name} ${it.name}" }})"

}