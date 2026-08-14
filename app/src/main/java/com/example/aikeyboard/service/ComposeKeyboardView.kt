package com.example.aikeyboard.service

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import com.example.aikeyboard.keyboard.AIKeyBoard

class ComposeKeyboardView (context: Context) : AbstractComposeView(context){
    @Composable
    override fun Content(){
        AIKeyBoard()
    }
}