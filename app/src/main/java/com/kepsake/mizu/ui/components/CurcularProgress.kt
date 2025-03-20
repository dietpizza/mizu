package com.kepsake.mizu.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.withStyledAttributes
import com.google.android.material.color.MaterialColors
import com.kepsake.mizu.R
import kotlin.math.min

class CircularProgress @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Default values
    private var strokeWidth = 12f
    private var progress = 0f
    private var max = 100f
    private var trackColor = Color.LTGRAY
    private var progressColor =
        MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary)

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val rectF = RectF()

    init {
        context.withStyledAttributes(attrs, R.styleable.CircularProgressBar) {
            strokeWidth = getDimension(R.styleable.CircularProgressBar_strokeWidth, strokeWidth)
            progress = getFloat(R.styleable.CircularProgressBar_progress, progress)
            max = getFloat(R.styleable.CircularProgressBar_max, max)
            trackColor = getColor(R.styleable.CircularProgressBar_trackColor, trackColor)
            progressColor = getColor(R.styleable.CircularProgressBar_progressColor, progressColor)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // Calculate the dimensions of the arc
        val diameter = min(width, height) - strokeWidth
        val radius = diameter / 2f

        // Calculate the bounds of the arc
        val centerX = width / 2f
        val centerY = height / 2f
        rectF.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        // Draw the track
        paint.color = trackColor
        paint.strokeWidth = strokeWidth
        canvas.drawArc(rectF, 0f, 360f, false, paint)

        // Draw the progress
        paint.color = progressColor
        paint.strokeWidth = strokeWidth
        val sweepAngle = 360 * (progress / max)
        canvas.drawArc(rectF, -90f, sweepAngle, false, paint)
    }

    /**
     * Set the progress of the circular progress bar
     * @param progress value between 0 and max
     */
    fun setProgress(progress: Float) {
        this.progress = progress.coerceIn(0f, max)
        invalidate()
    }

    /**
     * Set the maximum value of the progress bar
     * @param max maximum value
     */
    fun setMax(max: Float) {
        this.max = max
        invalidate()
    }

    /**
     * Get the current progress percentage
     * @return progress percentage (0-100)
     */
    fun getProgressPercentage(): Float {
        return (progress / max) * 100f
    }
}