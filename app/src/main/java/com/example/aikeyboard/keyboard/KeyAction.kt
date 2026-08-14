package com.example.aikeyboard.keyboard

sealed interface KeyAction {

    data class CommitText(

        val text: String

    ) : KeyAction



    data object Delete : KeyAction

    data object Done : KeyAction

    data object Enter : KeyAction

}