package com.flesiy.Lotus.ui.components.markdown

import android.graphics.Paint
import android.text.TextPaint
import android.text.style.MetricAffectingSpan

/**
 * A custom span for rendering Markdown headings with different sizes based on the heading level.
 *
 * @param level the heading level (1-6)
 */
class HeadingSpan(private val level: Int) : MetricAffectingSpan() {
    override fun updateMeasureState(textPaint: TextPaint) = update(textPaint)
    override fun updateDrawState(tp: TextPaint) = update(tp)

    private fun update(paint: Paint) {
        paint.apply {
            isFakeBoldText = true
            textSize *= when (level) {
                1 -> HEADING_1_SIZE_MULTIPLIER
                2 -> HEADING_2_SIZE_MULTIPLIER
                3 -> HEADING_3_SIZE_MULTIPLIER
                4 -> HEADING_4_SIZE_MULTIPLIER
                5 -> HEADING_5_SIZE_MULTIPLIER
                6 -> HEADING_6_SIZE_MULTIPLIER
                else -> 1f
            }
        }
    }

    companion object {
        private const val HEADING_1_SIZE_MULTIPLIER = 2.0f
        private const val HEADING_2_SIZE_MULTIPLIER = 1.75f
        private const val HEADING_3_SIZE_MULTIPLIER = 1.5f
        private const val HEADING_4_SIZE_MULTIPLIER = 1.25f
        private const val HEADING_5_SIZE_MULTIPLIER = 1.15f
        private const val HEADING_6_SIZE_MULTIPLIER = 1.1f
    }
} 