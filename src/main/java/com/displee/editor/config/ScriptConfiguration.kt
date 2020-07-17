package com.displee.editor.config

import dawn.cs2.unscramble.UnscrambleUtils

data class ScriptConfiguration(val version: Int, val opcodeDatabase: String, val disableSwitches: Boolean = false, val disableLongs: Boolean = false) {

	val scrambled = hashMapOf<Int, Int>()
	val unscrambled = hashMapOf<Int, Int>()

	init {
		UnscrambleUtils.read(version, scrambled, unscrambled)
	}

	override fun toString(): String {
		return "ScriptConfiguration[version=$version, database=$opcodeDatabase, switches=${!disableSwitches}, longs=${!disableLongs}]"
	}

}