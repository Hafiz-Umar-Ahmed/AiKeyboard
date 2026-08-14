    package com.example.aikeyboard.keyboard

    import java.util.UUID

    /** A single turn in the in-keyboard AI chat. */
    data class ChatMessage(
        val id: String = UUID.randomUUID().toString(),
        val role: Role,
        val text: String
    ) {
        enum class Role { USER, ASSISTANT }
    }