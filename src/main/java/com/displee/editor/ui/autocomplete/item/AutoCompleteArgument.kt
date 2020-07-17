package com.displee.editor.ui.autocomplete.item

import com.displee.editor.ui.autocomplete.AutoCompleteItem
import com.displee.editor.ui.autocomplete.AutoCompleteItemType
import dawn.cs2.CS2Type

class AutoCompleteArgument(name: String, returnType: CS2Type) : AutoCompleteItem(name, returnType, AutoCompleteItemType.ARGUMENT)