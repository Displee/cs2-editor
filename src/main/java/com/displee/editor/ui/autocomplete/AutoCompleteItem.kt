package com.displee.editor.ui.autocomplete

import com.google.gson.annotations.SerializedName
import dawn.cs2.CS2Type

open class AutoCompleteItem(var name: String,
					   @SerializedName("return_type")
					   val returnType: CS2Type,
					   val type: AutoCompleteItemType) {

	@SerializedName("display_name")
	open var displayName: String = ""
		get() {
			return if (field.isNullOrBlank()) { //kotlin and gson magic
				when (type) {
					AutoCompleteItemType.METHOD -> {
						"$name(...)"
					}
					else -> name
				}
			} else {
				field
			}
		}

	override fun toString(): String {
		return displayName
	}

}