package com.displee.editor.ui

import com.displee.editor.controller.MainController
import org.fxmisc.richtext.CodeArea
import java.time.Duration

fun CodeArea.buildStyle() {
	val cleanupWhenNoLongerNeedIt = richChanges().successionEnds(Duration.ofMillis(500)).subscribe {
		setStyleSpans(0, MainController.computeHighlighting(text))
	}
}