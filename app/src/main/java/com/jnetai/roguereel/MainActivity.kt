package com.jnetai.roguereel

import android.graphics.*
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.os.Bundle
import android.widget.ImageView
import android.widget.ScrollView
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*
import java.util.*
import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "RogueReel"
        const val CURRENT_VERSION = "1.0.0"
        const val GITHUB_REPO = "jnetai-clawbot/RogueReel"
    }

    private lateinit var gameView: GameView
    private lateinit var aboutButton: Button
    private lateinit var scoreText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF0A0A1A.toInt()
        window.navigationBarColor = 0xFF0A0A1A.toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A0A1A.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        scoreText = TextView(this).apply {
            text = "Depth: 0m"
            setTextColor(0xFF00FF88.toInt())
            textSize = 18f
            setPadding(32, 32, 32, 8)
            typeface = Typeface.MONOSPACE
        }

        gameView = GameView(this, ::updateScore)

        val buttonBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(16, 8, 16, 48)
        }

        val restartBtn = Button(this).apply {
            text = "Restart"
            setBackgroundColor(0xFF1A2A3A.toInt())
            setTextColor(0xFFCCCCCC.toInt())
            textSize = 14f
            minHeight = 0
            minimumHeight = 80
            setPadding(24, 12, 24, 12)
            setOnClickListener { gameView.restart() }
        }

        aboutButton = Button(this).apply {
            text = "About"
            setBackgroundColor(0xFF1A2A3A.toInt())
            setTextColor(0xFF00FF88.toInt())
            textSize = 14f
            minHeight = 0
            minimumHeight = 80
            setPadding(24, 12, 24, 12)
            setOnClickListener { showAbout() }
        }

        buttonBar.addView(restartBtn)
        val spacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(32, 0) }
        buttonBar.addView(spacer)
        buttonBar.addView(aboutButton)

        root.addView(scoreText)
        root.addView(gameView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        root.addView(buttonBar)
        setContentView(root)
    }

    private fun updateScore(depth: Int) {
        runOnUiThread {
            scoreText.text = "Depth: ${depth}m"
        }
    }

    private fun showAbout() {
        val builder = AlertDialog.Builder(this, R.style.AboutDialogTheme)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 32)
            setBackgroundColor(0xFF151528.toInt())
        }

        layout.addView(TextView(this).apply {
            text = "Rogue Reel"
            setTextColor(0xFF00FF88.toInt())
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 8)
        })

        layout.addView(TextView(this).apply {
            text = "Made by jnetai.com"
            setTextColor(0xFF888899.toInt())
            textSize = 14f
            setPadding(0, 0, 0, 16)
        })

        layout.addView(TextView(this).apply {
            text = "Version $CURRENT_VERSION"
            setTextColor(0xFFCCCCCC.toInt())
            textSize = 16f
            setPadding(0, 0, 0, 24)
        })

        val checkBtn = Button(this).apply {
            text = "Check for Update"
            setBackgroundColor(0xFF006644.toInt())
            setTextColor(0xFF00FF88.toInt())
            textSize = 15f
            minimumHeight = 96
            setPadding(32, 16, 32, 16)
            val btn = this
            setOnClickListener {
                btn.isEnabled = false
                btn.text = "Checking..."
                checkForUpdate { result ->
                    runOnUiThread {
                        btn.text = result
                        btn.isEnabled = true
                    }
                }
            }
        }
        layout.addView(checkBtn)

        layout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 24)
        })

        val shareBtn = Button(this).apply {
            text = "Share App"
            setBackgroundColor(0xFF234A6A.toInt())
            setTextColor(0xFF00CCFF.toInt())
            textSize = 15f
            minimumHeight = 96
            setPadding(32, 16, 32, 16)
            setOnClickListener {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Rogue Reel")
                    putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message))
                }
                startActivity(Intent.createChooser(intent, "Share via"))
            }
        }
        layout.addView(shareBtn)

        val scrollView = ScrollView(this).apply {
            addView(layout)
        }

        builder.setView(scrollView)
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun checkForUpdate(callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val latestTag = json.getString("tag_name").removePrefix("v")

                if (latestTag != CURRENT_VERSION) {
                    callback("New version $latestTag available!")
                } else {
                    callback("You're up to date!")
                }
            } catch (e: Exception) {
                callback("Could not check updates: ${e.message}")
            }
        }
    }
}

class GameView(context: Context, private val scoreCallback: (Int) -> Unit) : View(context) {
    companion object {
        const val TILE_SIZE = 60f
        const val CAVE_WIDTH = 20
        const val SONAR_RADIUS = 180f
        const val BLAST_RADIUS = 120f
        const val TAG = "GameView"
    }

    private val cave = Array(200) { BooleanArray(CAVE_WIDTH) }
    private val revealed = Array(200) { BooleanArray(CAVE_WIDTH) }
    private val enemies = mutableListOf<Enemy>()
    private var playerX = CAVE_WIDTH / 2f
    private var playerY = 15f
    private var currentDepth = 0
    private var gameOver = false
    private var sonarPings = mutableListOf<SonarPing>()
    private var directionalBlast: DirectionalBlast? = null
    private var longPressActive = false
    private var longPressX = 0f
    private var longPressY = 0f
    private val random = Random()

    private val wallPaint = Paint().apply { color = 0xFF1A2A3A.toInt(); style = Paint.Style.FILL }
    private val wallRevealedPaint = Paint().apply { color = 0xFF234A6A.toInt(); style = Paint.Style.FILL }
    private val bgPaint = Paint().apply { color = 0xFF0A0A1A.toInt(); style = Paint.Style.FILL }
    private val sonarPaint = Paint().apply { color = 0x3300FF88.toInt(); style = Paint.Style.FILL }
    private val enemyPaint = Paint().apply { color = 0xFFFF3344.toInt(); style = Paint.Style.FILL }
    private val enemyStunnedPaint = Paint().apply { color = 0xFF886622.toInt(); style = Paint.Style.FILL }
    private val playerPaint = Paint().apply { color = 0xFF00CCFF.toInt(); style = Paint.Style.FILL }
    private val playerGlowPaint = Paint().apply { color = 0x3300CCFF.toInt(); style = Paint.Style.FILL }
    private val blastPaint = Paint().apply { color = 0x6600FF88.toInt(); style = Paint.Style.FILL }
    private val pathPaint = Paint().apply { color = 0xFFFF8800.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f; pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f) }
    private val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }

    init {
        generateCave()
        startSonarReveal()
    }

    private fun generateCave() {
        for (y in 0 until 200) {
            for (x in 0 until CAVE_WIDTH) {
                cave[y][x] = true
            }
        }
        var cx = CAVE_WIDTH / 2f
        var cy = 0f
        while (cy < 190) {
            val r = 1.5f + random.nextFloat() * 3f
            for (y in max(0f, cy - r).toInt()..min(199f, cy + r).toInt()) {
                for (x in max(0f, cx - r).toInt()..min((CAVE_WIDTH - 1).toFloat(), cx + r).toInt()) {
                    cave[y][x] = false
                }
            }
            cx += (random.nextFloat() - 0.5f) * 2.5f
            cx = cx.coerceIn(1.5f, CAVE_WIDTH - 2.5f)
            cy += 1f + random.nextFloat() * 2f
        }
        for (y in 0..3) {
            for (x in (CAVE_WIDTH / 2 - 2)..(CAVE_WIDTH / 2 + 2)) {
                cave[y][x] = false
            }
        }
        for (i in 0 until 15) {
            val ey = 30 + random.nextInt(170)
            val ex = random.nextInt(CAVE_WIDTH)
            if (!cave[ey][ex]) {
                enemies.add(Enemy(ex.toFloat(), ey.toFloat()))
            }
        }
    }

    private fun startSonarReveal() {
        postDelayed({
            if (!gameOver) {
                sonarPings.add(SonarPing(playerX, playerY, SONAR_RADIUS * 0.25f, 500))
                startSonarReveal()
            }
        }, 2000)
    }

    private fun revealArea(cx: Float, cy: Float, radius: Float) {
        val tileR = radius / TILE_SIZE
        val minX = ((cx - tileR).toInt().coerceAtLeast(0))
        val maxX = ((cx + tileR).toInt().coerceAtMost(CAVE_WIDTH - 1))
        val minY = ((cy - tileR).toInt().coerceAtLeast(0))
        val maxY = ((cy + tileR).toInt().coerceAtMost(199))
        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val dist = sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy))
                if (dist <= tileR) {
                    revealed[y][x] = true
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gameOver) {
            if (event.action == MotionEvent.ACTION_DOWN) restart()
            return true
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                longPressActive = true
                longPressX = event.x
                longPressY = event.y
                postDelayed({
                    if (longPressActive) {
                        sonarPings.add(SonarPing(playerX, playerY, SONAR_RADIUS, 1200))
                        val dx = longPressX - width / 2f
                        val dy = longPressY - height / 2f
                        val angle = atan2(dy.toDouble(), dx.toDouble()).toFloat()
                        sonarPings.add(SonarPing(
                            playerX + cos(angle) * 1.5f,
                            playerY + sin(angle) * 1.5f,
                            BLAST_RADIUS,
                            600
                        ))
                        val blast = DirectionalBlast(playerX, playerY, angle, BLAST_RADIUS)
                        directionalBlast = blast
                        for (enemy in enemies) {
                            if (enemy.stunned <= 0) {
                                val edx = enemy.x - playerX
                                val edy = enemy.y - playerY
                                val dist = sqrt(edx * edx + edy * edy)
                                val enemyAngle = atan2(edy.toDouble(), edx.toDouble()).toFloat()
                                val angleDiff = abs(((angle - enemyAngle + PI * 3) % (PI * 2) - PI).toFloat())
                                if (dist < BLAST_RADIUS / TILE_SIZE && angleDiff < PI / 4f) {
                                    enemy.stunned = 5000
                                }
                            }
                        }
                    }
                }, 600)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                longPressActive = false
                removeCallbacks(null)
                if (event.eventTime - event.downTime < 400) {
                    sonarPings.add(SonarPing(playerX, playerY, SONAR_RADIUS, 1000))
                    for (enemy in enemies) {
                        if (enemy.stunned <= 0) {
                            val dx = enemy.x - playerX
                            val dy = enemy.y - playerY
                            val dist = sqrt(dx * dx + dy * dy)
                            if (dist < SONAR_RADIUS / TILE_SIZE * 0.8f) {
                                enemy.speed = minOf(enemy.speed * 1.3f, 0.05f)
                            }
                        }
                    }
                }
            }
        }
        return true
    }

    private val gameLoop = object : Runnable {
        override fun run() {
            if (gameOver) return
            update()
            invalidate()
            postDelayed(this, 33)
        }
    }

    init {
        post(gameLoop)
    }

    private fun update() {
        playerY += 0.03f
        if (playerY.toInt() % 10 == 0 && playerY.toInt() != currentDepth) {
            currentDepth = playerY.toInt()
            scoreCallback(currentDepth)
        }
        if (playerY.toInt() in cave.indices) {
            val px = playerX.toInt().coerceIn(0, CAVE_WIDTH - 1)
            if (cave[playerY.toInt()][px]) {
                gameOver = true
            }
        }
        if (playerY >= 199f) {
            gameOver = true
        }

        val iter = enemies.iterator()
        while (iter.hasNext()) {
            val e = iter.next()
            if (e.stunned > 0) {
                e.stunned -= 33
                continue
            }
            val dx = playerX - e.x
            val dy = playerY - e.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < 0.6f) {
                gameOver = true
                return
            }
            if (dist > 0.2f) {
                e.x += (dx / dist * e.speed).toFloat()
                e.y += (dy / dist * e.speed * 0.5f).toFloat()
            }
            e.x = e.x.coerceIn(0f, CAVE_WIDTH - 1f)
            e.y = e.y.coerceIn(0f, 199f)
        }

        val pingIter = sonarPings.iterator()
        while (pingIter.hasNext()) {
            val p = pingIter.next()
            val maxDist = p.radius / TILE_SIZE
            val progress = p.lerp()
            val currentRadius = maxDist * progress
            revealArea(p.x, p.y, currentRadius)
            if (progress >= 1f && p.elapsed > p.duration) {
                pingIter.remove()
            }
            p.elapsed += 33
        }

        directionalBlast?.let {
            it.elapsed += 33
            if (it.elapsed > 600) directionalBlast = null
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val viewHeight = height.toFloat()
        val viewWidth = width.toFloat()
        val offsetX = (viewWidth - CAVE_WIDTH * TILE_SIZE) / 2f
        val viewCenterY = viewHeight / 2f
        val offsetY = viewCenterY - playerY * TILE_SIZE

        val yTilesVisible = (viewHeight / TILE_SIZE).toInt() + 2
        val startY = max(0, playerY.toInt() - yTilesVisible / 2)
        val endY = min(199, startY + yTilesVisible)

        for (y in startY..endY) {
            for (x in 0 until CAVE_WIDTH) {
                if (revealed[y][x]) {
                    if (cave[y][x]) {
                        canvas.drawRect(
                            offsetX + x * TILE_SIZE,
                            offsetY + y * TILE_SIZE,
                            offsetX + (x + 1) * TILE_SIZE - 1,
                            offsetY + (y + 1) * TILE_SIZE - 1,
                            wallRevealedPaint
                        )
                    }
                } else {
                    canvas.drawRect(
                        offsetX + x * TILE_SIZE,
                        offsetY + y * TILE_SIZE,
                        offsetX + (x + 1) * TILE_SIZE - 1,
                        offsetY + (y + 1) * TILE_SIZE - 1,
                        bgPaint
                    )
                }
            }
        }

        for (e in enemies) {
            if (revealed[e.y.toInt().coerceIn(0, 199)][e.x.toInt().coerceIn(0, CAVE_WIDTH - 1)]) {
                val paint = if (e.stunned > 0) enemyStunnedPaint else enemyPaint
                canvas.drawCircle(
                    offsetX + e.x * TILE_SIZE,
                    offsetY + e.y * TILE_SIZE,
                    TILE_SIZE * 0.35f,
                    paint
                )
            }
        }

        val px = offsetX + playerX * TILE_SIZE
        val py = offsetY + playerY * TILE_SIZE
        canvas.drawCircle(px, py, TILE_SIZE * 0.35f, playerGlowPaint)
        canvas.drawCircle(px, py, TILE_SIZE * 0.25f, playerPaint)

        directionalBlast?.let { blast ->
            val progress = blast.elapsed / 600f
            val angle = blast.angle
            val rad = blast.radius * (1f - progress)
            val path = Path()
            path.moveTo(px, py)
            path.arcTo(
                px - rad, py - rad, px + rad, py + rad,
                Math.toDegrees((-angle + PI / 8).toDouble()).toFloat(),
                45f * (1f - progress),
                false
            )
            path.close()
            canvas.drawPath(path, blastPaint)
        }

        for (ping in sonarPings) {
            val cx = offsetX + ping.x * TILE_SIZE
            val cy = offsetY + ping.y * TILE_SIZE
            val maxDist = ping.radius
            val currentR = maxDist * ping.lerp()
            val alpha = ((1f - ping.lerp()) * 0.4).toInt()
            sonarPaint.alpha = (alpha * 255).toInt()
            canvas.drawCircle(cx, cy, currentR, sonarPaint)
        }

        if (gameOver) {
            val overlay = Paint().apply { color = 0xBB000000.toInt(); style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, viewWidth, viewHeight, overlay)
            val textPaint = Paint().apply {
                color = 0xFFFF3344.toInt()
                textSize = 48f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            val depthText = "Game Over"
            canvas.drawText(depthText, viewWidth / 2f, viewHeight / 2f - 24, textPaint)
            textPaint.textSize = 28f
            textPaint.color = 0xFFCCCCCC.toInt()
            val restartText = "Tap to Restart"
            canvas.drawText(restartText, viewWidth / 2f, viewHeight / 2f + 32, textPaint)
        }
    }

    fun restart() {
        revealed.forEach { it.fill(false) }
        enemies.clear()
        generateCave()
        playerX = CAVE_WIDTH / 2f
        playerY = 15f
        currentDepth = 0
        gameOver = false
        sonarPings.clear()
        directionalBlast = null
        scoreCallback(0)
        invalidate()
    }
}

data class Enemy(var x: Float, var y: Float, var speed: Float = 0.025f, var stunned: Int = 0)

data class SonarPing(val x: Float, val y: Float, val radius: Float, val duration: Int, var elapsed: Int = 0) {
    fun lerp(): Float = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
}

data class DirectionalBlast(val x: Float, val y: Float, val angle: Float, val radius: Float, var elapsed: Int = 0)
