package com.example.aikeyboard.service

import android.view.inputmethod.InputConnection

/**
 * Helpers for reading and replacing text in the currently focused input field.
 * These wrap InputConnection's quirks (selection vs. whole-field, batch edits)
 * so AI features don't need to deal with them directly.
 */
object TextFieldAccessor {

    private const val MAX_CHARS = 8000

    /**
     * Returns the text an AI feature should act on: the current selection if
     * there is one, otherwise the entire field content. Null if the field is
     * empty or there's no active connection.
     */
    fun getWorkingText(ic: InputConnection?): String? {
        if (ic == null) return null

        val selected = ic.getSelectedText(0)?.toString()
        if (!selected.isNullOrEmpty()) return selected

        val before = ic.getTextBeforeCursor(MAX_CHARS, 0)?.toString().orEmpty()
        val after = ic.getTextAfterCursor(MAX_CHARS, 0)?.toString().orEmpty()
        val full = before + after
        return full.ifBlank { null }
    }

    /** True if the user currently has an active text selection (vs. just a cursor). */
    fun hasSelection(ic: InputConnection?): Boolean =
        !ic?.getSelectedText(0)?.toString().isNullOrEmpty()

    /**
     * Replaces whatever [getWorkingText] would currently return with [newText]:
     * the selection if one was active when this is called, otherwise the whole
     * field content.
     */
    fun replaceWorkingText(ic: InputConnection?, newText: String) {
        if (ic == null) return

        ic.beginBatchEdit()
        try {
            val selected = ic.getSelectedText(0)?.toString()
            if (!selected.isNullOrEmpty()) {
                // commitText replaces the active selection in standard InputConnections.
                ic.commitText(newText, 1)
            } else {
                val before = ic.getTextBeforeCursor(MAX_CHARS, 0)?.toString().orEmpty()
                val after = ic.getTextAfterCursor(MAX_CHARS, 0)?.toString().orEmpty()
                if (before.isNotEmpty() || after.isNotEmpty()) {
                    ic.deleteSurroundingText(before.length, after.length)
                }
                ic.commitText(newText, 1)
            }
        } finally {
            ic.endBatchEdit()
        }
    }

    /**
     * Inserts [text] at the current cursor position without touching the rest
     * of the field — used by features that add to the text (e.g. "Continue
     * writing") rather than replace it.
     */
    fun insertAtCursor(ic: InputConnection?, text: String) {
        if (ic == null) return
        ic.beginBatchEdit()
        try {
            ic.commitText(text, 1)
        } finally {
            ic.endBatchEdit()
        }
    }
}