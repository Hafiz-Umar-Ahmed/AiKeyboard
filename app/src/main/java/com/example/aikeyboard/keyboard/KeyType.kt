package com.example.aikeyboard.keyboard

import androidx.compose.ui.graphics.vector.ImageVector

sealed class KeyType(

    open val description: Int? = null,

    open val showDescription: Boolean = false

) {

    data class KeyIcon(

        val icon: ImageVector,

        override val description: Int? = null,

        override val showDescription: Boolean = false

    ) : KeyType(description, showDescription)



    data class KeyText(

        val value: String,

        override val description: Int? = null,

        override val showDescription: Boolean = false

    ) : KeyType(description, showDescription)

}