package com.marko.turista

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class CompassView(context: Context) : View(context) {

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val redPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var azimuth = 0f

    init {
        circlePaint.color = Color.argb(185, 15, 22, 25)
        circlePaint.style = Paint.Style.FILL

        ringPaint.color = Color.WHITE
        ringPaint.style = Paint.Style.STROKE
        ringPaint.strokeWidth = 5f

        tickPaint.color = Color.WHITE
        tickPaint.strokeWidth = 3f

        textPaint.color = Color.WHITE
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 32f
        textPaint.isFakeBoldText = true

        smallTextPaint.color = Color.LTGRAY
        smallTextPaint.textAlign = Paint.Align.CENTER
        smallTextPaint.textSize = 18f

        redPaint.color = Color.RED
        redPaint.style = Paint.Style.FILL

        whitePaint.color = Color.WHITE
        whitePaint.style = Paint.Style.FILL
    }

    fun setAzimuth(angle: Float) {
        azimuth = angle
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f

        val radius = minOf(width, height) / 2f - 12f

        // Priehľadné pozadie kompasu
        canvas.drawCircle(
            centerX,
            centerY,
            radius,
            circlePaint
        )

        // Vonkajší kruh
        canvas.drawCircle(
            centerX,
            centerY,
            radius,
            ringPaint
        )

        canvas.save()

        // Otáčanie stupnice podľa smeru telefónu
        canvas.rotate(
            -azimuth,
            centerX,
            centerY
        )

        // Stupnica po 10 stupňoch
        for (degree in 0 until 360 step 10) {

            val angle = Math.toRadians(degree.toDouble())

            val isMajor = degree % 30 == 0

            val outerRadius = radius - 10f

            val innerRadius =
                if (isMajor) {
                    radius - 32f
                } else {
                    radius - 23f
                }

            val startX =
                centerX +
                        sin(angle).toFloat() *
                        innerRadius

            val startY =
                centerY -
                        cos(angle).toFloat() *
                        innerRadius

            val endX =
                centerX +
                        sin(angle).toFloat() *
                        outerRadius

            val endY =
                centerY -
                        cos(angle).toFloat() *
                        outerRadius

            tickPaint.strokeWidth =
                if (isMajor) 4f else 2f

            canvas.drawLine(
                startX,
                startY,
                endX,
                endY,
                tickPaint
            )
        }

        // Slovenské svetové strany
        drawDirection(
            canvas,
            "S",
            0,
            centerX,
            centerY,
            radius
        )

        drawDirection(
            canvas,
            "V",
            90,
            centerX,
            centerY,
            radius
        )

        drawDirection(
            canvas,
            "J",
            180,
            centerX,
            centerY,
            radius
        )

        drawDirection(
            canvas,
            "Z",
            270,
            centerX,
            centerY,
            radius
        )

        canvas.restore()

        // Pevná červená šípka hore
        val arrow = Path()

        arrow.moveTo(
            centerX,
            centerY - radius + 3f
        )

        arrow.lineTo(
            centerX - 18f,
            centerY - radius + 38f
        )

        arrow.lineTo(
            centerX + 18f,
            centerY - radius + 38f
        )

        arrow.close()

        canvas.drawPath(
            arrow,
            redPaint
        )

        // Stred kompasu
        canvas.drawCircle(
            centerX,
            centerY,
            10f,
            redPaint
        )

        canvas.drawCircle(
            centerX,
            centerY,
            5f,
            whitePaint
        )

        // Aktuálny smer
        val direction = getDirectionName(azimuth)

        canvas.drawText(
            "${azimuth.toInt()}°  $direction",
            centerX,
            centerY + radius - 35f,
            smallTextPaint
        )
    }

    private fun drawDirection(
        canvas: Canvas,
        text: String,
        degree: Int,
        centerX: Float,
        centerY: Float,
        radius: Float
    ) {

        val angle = Math.toRadians(degree.toDouble())

        val textRadius = radius - 65f

        val x =
            centerX +
                    sin(angle).toFloat() *
                    textRadius

        val y =
            centerY -
                    cos(angle).toFloat() *
                    textRadius +
                    11f

        canvas.drawText(
            text,
            x,
            y,
            textPaint
        )
    }

    private fun getDirectionName(degrees: Float): String {

        return when {
            degrees >= 337.5f || degrees < 22.5f ->
                "Sever"

            degrees < 67.5f ->
                "SV"

            degrees < 112.5f ->
                "Východ"

            degrees < 157.5f ->
                "JV"

            degrees < 202.5f ->
                "Juh"

            degrees < 247.5f ->
                "JZ"

            degrees < 292.5f ->
                "Západ"

            else ->
                "SZ"
        }
    }
}