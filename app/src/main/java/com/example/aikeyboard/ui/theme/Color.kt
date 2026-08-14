package com.example.aikeyboard.ui.theme

import androidx.compose.ui.graphics.Color

// Two-color brand system: white/near-black as the primary surface, one blue
// accent for interactive/active elements. No third color anywhere.

// --- Light theme ---
val BlueAccentLight = Color(0xFF3D5AFE)
val LightIcon = Color(0xFFFFFFFF)
val PaleBluePage = Color(0xFFF3F5FC)       // overall keyboard background (page)
val KeyWhite = Color(0xFFFFFFFF)           // individual key bubbles
val NavyText = Color(0xFF31355A)           // key letters / body text
val MutedNavy = Color(0xFF6C7094)          // secondary text
val LightOutline = Color(0xFFE4E7F2)
val PaleBlueContainer = Color(0xFFE8ECFF)  // selected chip / container fill

// --- Dark theme ---
val BlueAccentDark = Color(0xFF7C93FF)
val DarkIconColor = Color.Black
val NearBlackPage = Color(0xFF17181D)      // overall keyboard background (page)
val KeyDark = Color(0xFF25262F)            // individual key bubbles — lighter than page so they "float"
val OffWhiteText = Color(0xFFE8E9F3)
val MutedLavender = Color(0xFF9B9FC2)
val DarkOutline = Color(0xFF34364A)
val DeepBlueContainer = Color(0xFF2C3157)