package com.example.aikeyboard.ai

object TokenEstimator {
    private const val CHARS_PER_TOKEN = 4.0

    // Increased to 2.5 because some tones (like Formal) naturally expand the text
    private const val REWRITE_MULTIPLIER = 2.5
    private const val MIN_REWRITE_TOKENS = 256

    /** Used ONLY for Tone Rewrites where output size correlates to input size */
    fun estimateRewriteTokens(sourceText: String): Int {
        val estimatedInputTokens = (sourceText.length / CHARS_PER_TOKEN).toInt().coerceAtLeast(1)
        val target = (estimatedInputTokens * REWRITE_MULTIPLIER).toInt()
        return target.coerceAtLeast(MIN_REWRITE_TOKENS)
    }

    /** Used for Chat where a 5-word prompt might require a 500-word answer */
    fun getChatMaxTokens(): Int {
        return 2048 // Generous ceiling that prevents mid-sentence cutoffs
    }
}