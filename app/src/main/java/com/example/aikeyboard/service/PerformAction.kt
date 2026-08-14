package com.example.aikeyboard.service

import android.util.Log
import android.view.KeyEvent
import com.example.aikeyboard.keyboard.KeyAction

fun performKeyAction(

    action: KeyAction,

    ime: KeyboardService? = null

) {

    when (action) {

        is KeyAction.CommitText -> {

            val text = action.text

            ime?.currentInputConnection?.commitText(

                text,

                1,

                )

            Log.d("Test", "committing key text: $text")

        }



        is KeyAction.Delete -> {

            val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)

            ime?.currentInputConnection?.sendKeyEvent(event)

            Log.d("Test", "delete")

        }



        KeyAction.Done -> {

            ime?.requestHideSelf(0)

            Log.d("Test", "hide")

        }



        KeyAction.Enter -> {

            val event = KeyEvent(

                KeyEvent.ACTION_DOWN,

                KeyEvent.KEYCODE_ENTER,

                )

            ime?.currentInputConnection?.sendKeyEvent(

                event

            )

            Log.d("Test", "Enter")

        }

    }

}