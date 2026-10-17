package com.example.bouncelab

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(8, 13, 29)
        window.navigationBarColor = Color.rgb(8, 13, 29)
        setContentView(BounceLabView(this))
    }
}

private data class Bubble(
    val x: Float,
    var y: Float,
    val startY: Float,
    val groundY: Float,
    val radius: Float,
    val color: Int,
    val shadowColor: Int,
    var scaleX: Float = 1f,
    var scaleY: Float = 1f,
    var alpha: Float = 1f,
    var animator: ValueAnimator? = null
)

private class BounceLabView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private fun dp(value: Float): Float = value * density

    private val random = Random(System.nanoTime())
    private val bubbles = mutableListOf<Bubble>()
    private val palette = intArrayOf(
        Color.rgb(91, 232, 213),
        Color.rgb(116, 157, 255),
        Color.rgb(255, 113, 174),
        Color.rgb(255, 196, 100),
        Color.rgb(180, 139, 255),
        Color.rgb(103, 214, 255)
    )
    private val stars = List(46) {
        Triple(random.nextFloat(), random.nextFloat(), random.nextFloat())
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }
    private val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(28, 165, 190, 255)
    }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(109, 230, 213)
    }
    private val floorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(60, 163, 186, 235)
        strokeWidth = dp(1f)
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = dp(24f)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(169, 182, 211)
        textSize = dp(13f)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(205, 218, 245)
        textSize = dp(11f)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(135, 151, 184)
        textSize = dp(10f)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private var backgroundShader: Shader? = null
    private var lastSpawnX = Float.NaN
    private var lastSpawnY = Float.NaN

    init {
        isClickable = true
        contentDescription = "Bounce Lab. Tap the screen to create colorful bouncing bubbles."
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        backgroundShader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(
                Color.rgb(10, 16, 36),
                Color.rgb(16, 25, 51),
                Color.rgb(6, 10, 24)
            ),
            floatArrayOf(0f, 0.54f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        backgroundPaint.shader = backgroundShader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        backgroundPaint.shader = null

        stars.forEach { star ->
            val starAlpha = (30 + star.third * 75).toInt()
            backgroundPaint.color = Color.argb(starAlpha, 174, 199, 255)
            val radius = if (star.third > 0.8f) dp(1.3f) else dp(0.65f)
            canvas.drawCircle(star.first * width, star.second * height, radius, backgroundPaint)
        }

        canvas.drawText("BOUNCE LAB", dp(22f), dp(55f), titlePaint)
        canvas.drawText("Tap anywhere to make a bubble", dp(23f), dp(80f), subtitlePaint)

        canvas.drawRoundRect(
            dp(20f), dp(105f), width - dp(20f), dp(153f),
            dp(16f), dp(16f), chipPaint
        )
        canvas.drawCircle(dp(38f), dp(129f), dp(4f), accentPaint)
        canvas.drawText("TOUCH  •  BOUNCE  •  REPEAT", dp(52f), dp(133f), labelPaint)

        val floorY = height - dp(31f)
        canvas.drawLine(dp(20f), floorY, width - dp(20f), floorY, floorPaint)

        bubbles.forEach { bubble ->
            canvas.save()
            canvas.translate(bubble.x, bubble.y)
            canvas.scale(bubble.scaleX, bubble.scaleY)

            bubblePaint.alpha = (255f * bubble.alpha).toInt().coerceIn(0, 255)
            bubblePaint.shader = RadialGradient(
                -bubble.radius * 0.34f,
                -bubble.radius * 0.38f,
                bubble.radius * 1.48f,
                bubble.color,
                bubble.shadowColor,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(0f, 0f, bubble.radius, bubblePaint)
            bubblePaint.shader = null
            bubblePaint.alpha = 255

            glintPaint.alpha = (112f * bubble.alpha).toInt().coerceIn(0, 255)
            canvas.drawOval(
                -bubble.radius * 0.48f,
                -bubble.radius * 0.49f,
                -bubble.radius * 0.06f,
                -bubble.radius * 0.27f,
                glintPaint
            )
            glintPaint.alpha = (180f * bubble.alpha).toInt().coerceIn(0, 255)
            canvas.drawCircle(
                -bubble.radius * 0.38f,
                -bubble.radius * 0.22f,
                bubble.radius * 0.065f,
                glintPaint
            )
            glintPaint.alpha = 255
            canvas.restore()
        }

        val bubbleCount = bubbles.size.toString() + " ACTIVE BUBBLES"
        canvas.drawText(bubbleCount, dp(20f), height - dp(14f), footerPaint)
        val hint = "TAP TO CREATE"
        canvas.drawText(
            hint,
            width - dp(20f) - footerPaint.measureText(hint),
            height - dp(14f),
            footerPaint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastSpawnX = event.x
                lastSpawnY = event.y
                spawnBubble(event.x, event.y)
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (abs(event.x - lastSpawnX) >= dp(30f) ||
                    abs(event.y - lastSpawnY) >= dp(30f)
                ) {
                    spawnBubble(event.x, event.y)
                    lastSpawnX = event.x
                    lastSpawnY = event.y
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                lastSpawnX = Float.NaN
                lastSpawnY = Float.NaN
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun spawnBubble(touchX: Float, touchY: Float) {
        val radius = dp(random.nextInt(25, 38).toFloat())
        val minX = radius
        val maxX = (width.toFloat() - radius).coerceAtLeast(minX)
        val x = touchX.coerceIn(minX, maxX)

        val minY = dp(170f) + radius
        val maxY = (height.toFloat() - radius - dp(35f)).coerceAtLeast(minY)
        val y = touchY.coerceIn(minY, maxY)
        val groundY = (height.toFloat() - dp(35f) - radius).coerceAtLeast(y)
        val color = palette[random.nextInt(palette.size)]
        val shadow = Color.rgb(
            Color.red(color) / 3,
            Color.green(color) / 3,
            Color.blue(color) / 3
        )

        val bubble = Bubble(
            x = x,
            y = y,
            startY = y,
            groundY = groundY,
            radius = radius,
            color = color,
            shadowColor = shadow
        )
        bubbles.add(bubble)

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = random.nextLong(1350L, 1751L)
            interpolator = LinearInterpolator()
            addUpdateListener { valueAnimator ->
                updateBubble(bubble, valueAnimator.animatedValue as Float)
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    bubbles.remove(bubble)
                    invalidate()
                }
            })
        }
        bubble.animator = animator
        animator.start()
        invalidate()
    }

    private fun updateBubble(bubble: Bubble, progress: Float) {
        when {
            progress < 0.46f -> {
                val t = progress / 0.46f
                bubble.y = bubble.startY + (bubble.groundY - bubble.startY) * t * t
                bubble.scaleX = 1f + 0.035f * t
                bubble.scaleY = 1f - 0.025f * t
            }

            progress < 0.58f -> {
                val t = ((progress - 0.46f) / 0.12f).coerceIn(0f, 1f)
                val squash = sin(t.toDouble() * PI).toFloat()
                bubble.y = bubble.groundY
                bubble.scaleX = 1f + 0.34f * squash
                bubble.scaleY = 1f - 0.40f * squash
            }

            progress < 0.84f -> {
                val t = ((progress - 0.58f) / 0.26f).coerceIn(0f, 1f)
                val bounce = sin(t.toDouble() * PI).toFloat()
                bubble.y = bubble.groundY - dp(94f) * bounce
                bubble.scaleX = 1f - 0.035f * bounce
                bubble.scaleY = 1f + 0.08f * bounce
            }

            else -> {
                val t = ((progress - 0.84f) / 0.16f).coerceIn(0f, 1f)
                bubble.y = bubble.groundY
                bubble.alpha = 1f - t
                bubble.scaleX = 1f - 0.22f * t
                bubble.scaleY = 1f - 0.22f * t
            }
        }
    }

    override fun onDetachedFromWindow() {
        bubbles.toList().forEach { it.animator?.cancel() }
        bubbles.clear()
        super.onDetachedFromWindow()
    }
}
