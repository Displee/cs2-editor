package com.displee.editor.ui.autocomplete.item

import com.displee.editor.ui.autocomplete.AutoCompleteItem
import com.google.gson.annotations.SerializedName
import dawn.cs2.CS2Type

class AutoCompleteClass(@SerializedName("type") val returnType: CS2Type) {

	private val children = arrayOf<AutoCompleteItem>()

	var dynamicChildren = mutableListOf<AutoCompleteItem>()

	constructor(): this(CS2Type.UNKNOWN) { //cuz gson magic
		this.dynamicChildren = mutableListOf()
	}

	fun children(): List<AutoCompleteItem> {
		val list = children.toMutableList()
		list.addAll(dynamicChildren)
		return list
	}

}