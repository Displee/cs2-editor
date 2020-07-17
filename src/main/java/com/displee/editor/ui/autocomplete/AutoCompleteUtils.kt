package com.displee.editor.ui.autocomplete

import com.displee.editor.ui.autocomplete.item.AutoCompleteArgument
import com.displee.editor.ui.autocomplete.item.AutoCompleteClass
import com.displee.editor.ui.autocomplete.item.AutoCompleteFunction
import com.google.gson.*
import dawn.cs2.CS2Type
import java.io.InputStreamReader
import java.lang.IllegalStateException
import java.lang.reflect.Type

object AutoCompleteUtils {

	private val gson = Gson().newBuilder()
			.registerTypeAdapter(CS2Type::class.java, CS2TypeDeserializer())
			.registerTypeAdapter(AutoCompleteItem::class.java, AutoCompleteItemDeserializer())
			.setPrettyPrinting()
			.create()
	private val items = mutableListOf<AutoCompleteItem>()
	val dynamicItems = mutableListOf<AutoCompleteItem>()
	private val objects = mutableListOf<AutoCompleteClass>()

	init {
		loadAutoCompleteItems()
		loadAutoCompleteObjects()
	}

	fun forItem(name: String): AutoCompleteItem? {
		val formattedName = name.replace("\\(.*?\\)".toRegex(), "")
		for(i in items) {
			if (i.name == formattedName) {
				val valid =
						i.type != AutoCompleteItemType.METHOD ||
						i.type == AutoCompleteItemType.METHOD && name.contains("(") && name.contains(")")
				if (valid) {
					return i
				}
			}
		}
		return null
	}

	fun rootItems(): List<AutoCompleteItem> {
		val list = items.toMutableList()
		list.addAll(dynamicItems)
		return list
	}

	fun getObject(type: CS2Type, create: Boolean = false): AutoCompleteClass? {
		var obj = objects.firstOrNull { it.returnType == type }
		if (create && obj == null) {
			obj = AutoCompleteClass(type)
			objects.add(obj)
		}
		return obj
	}

	fun clearDynamicChildren() {
		objects.forEach { it.dynamicChildren.clear() }
	}

	private fun loadAutoCompleteItems() {
		val reader = InputStreamReader(javaClass.getResourceAsStream("/auto_complete_items.json"))
		val array = gson.fromJson(reader, Array<AutoCompleteItem>::class.java)
		items.addAll(array)
	}

	private fun loadAutoCompleteObjects() {
		val reader = InputStreamReader(javaClass.getResourceAsStream("/auto_complete_objects.json"))
		val array = gson.fromJson(reader, Array<AutoCompleteClass>::class.java)
		objects.addAll(array)
	}

	private class CS2TypeDeserializer: JsonDeserializer<CS2Type> {
		override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): CS2Type {
			return CS2Type.forDesc(json.asString) ?: throw IllegalStateException("No type found for: ${json.asString}")
		}
	}

	private class AutoCompleteItemDeserializer: JsonDeserializer<AutoCompleteItem> {
		override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): AutoCompleteItem {
			json as JsonObject
			val name = json["name"].asString
			val returnType = CS2Type.forDesc(json["return_type"].asString)
			return when(val type = AutoCompleteItemType.valueOf(json["type"].asString)) {
				AutoCompleteItemType.METHOD -> {
					val args = if (json.has("arguments")) {
						gson.fromJson(json["arguments"], Array<AutoCompleteArgument>::class.java)
					} else {
						arrayOf()
					}
					AutoCompleteFunction(name, returnType, args)
				} else -> {
					AutoCompleteItem(name, returnType, type)
				}
			}
		}
	}

}