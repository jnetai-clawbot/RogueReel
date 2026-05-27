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
    private lateinit var comboText: TextView

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

        val hudBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(32, 32, 32, 8)
        }

        scoreText = TextView(this).apply {
            text = "Score: 0"
            setTextColor(0xFFFFDD00.toInt())
            textSize = 16f
            typeface = Typeface.MONOSPACE
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        comboText = TextView(this).apply {
            text = ""
            setTextColor(0xFFFF8800.toInt())
            textSize = 14f
            typeface = Typeface.MONOSPACE
            gravity = android.view.Gravity.END
        }

        hudBar.addView(scoreText)
        hudBar.addView(comboText)

        gameView = GameView(this, ::updateHud)

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
            setTextColor(0xFFFFDD00.toInt())
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

        root.addView(hudBar)
        root.addView(gameView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        root.addView(buttonBar)
        setContentView(root)
    }

    private fun updateHud(score: Int, combo: String) {
        runOnUiThread {
            scoreText.text = "Score: $score"
            comboText.text = combo
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
            setTextColor(0xFFFFDD00.toInt())
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

enum class AttackType(val label: String, val genre: String, val damage: Int, val color: Int) {
    PUNCH("PUNCH", "ACTION", 8, 0xFFFF4444.toInt()),
    KICK("KICK", "WESTERN", 10, 0xFFCC8844.toInt()),
    SPECIAL("SPECIAL", "SCI-FI", 15, 0xFF44AAFF.toInt())
}

data class AttackEffect(var x: Float, var y: Float, var type: AttackType, var elapsed: Int = 0, val duration: Int = 500)
data class GlitchParticle(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Int, val maxLife: Int)
data class FilmFrame(val type: AttackType, val time: Long)

class GameView(context: Context, private val hudCallback: (Int, String) -> Unit) : View(context) {
    companion object {
        const val TAG = "GameView"
        const val MAX_HEALTH = 100f
        const val ENEMY_MAX_HEALTH = 80f
    }

    private val random = Random()
    private var playerHealth = MAX_HEALTH
    private var enemyHealth = ENEMY_MAX_HEALTH
    private var score = 0
    private var gameOver = false
    private var gameOverWon = false
    private var attackCooldown = 0
    private var enemyAttackTimer = 0
    private var lastTapTime = 0L
    private var lastTapSide = ""
    private var currentCombo = ""
    private val attackHistory = mutableListOf<FilmFrame>()
    private val attackEffects = mutableListOf<AttackEffect>()
    private val glitchParticles = mutableListOf<GlitchParticle>()
    private val frameParticles = mutableListOf<Pair<Float, Float>>()
    private var shakeAmount = 0f
    private var enemyGlitchOffset = 0f
    private var enemyGlitchTimer = 0
    private var bonusText = ""
    private var bonusTextTimer = 0
    private var comboMultiplier = 1f
    private var comboMultiplierTimer = 0

    private val bgPaint = Paint().apply {
        color = 0xFF0A0A1A.toInt()
        style = Paint.Style.FILL
    }
    private val playerReelPaint = Paint().apply {
        color = 0xFFFFDD00.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val playerCorePaint = Paint().apply {
        color = 0xFFFFAA00.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val playerSprocketPaint = Paint().apply {
        color = 0xFFFFDD00.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val filmStripPaint = Paint().apply {
        color = 0xFFFFDD00.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val filmStripLostPaint = Paint().apply {
        color = 0xFF332200.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val filmBorderPaint = Paint().apply {
        color = 0xFFFFDD00.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    private val enemyPaint = Paint().apply {
        color = 0xFFFF3366.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    private val enemyCorePaint = Paint().apply {
        color = 0xFFFF1144.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val glitchPaint = Paint().apply {
        color = 0x44FF1144.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val attackTextPaint = Paint().apply {
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
    }
    private val bonusTextPaint = Paint().apply {
        textSize = 22f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    private val hudPaint = Paint().apply {
        color = 0xFFFFDD00.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val healthBgPaint = Paint().apply {
        color = 0xFF2A2A3A.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val enemyHealthBgPaint = Paint().apply {
        color = 0xFF2A2A3A.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val particlePaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val dividerPaint = Paint().apply {
        color = 0x22FFDD00.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    init {
        spawnInitialGlitchParticles()
    }

    private fun spawnInitialGlitchParticles() {
        for (i in 0..30) {
            val w = 400f
            val h = 600f
            glitchParticles.add(GlitchParticle(
                random.nextFloat() * w, random.nextFloat() * h,
                (random.nextFloat() - 0.5f) * 2f, (random.nextFloat() - 0.5f) * 2f,
                random.nextInt(40) + 10, 50
            ))
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gameOver) {
            if (event.action == MotionEvent.ACTION_DOWN) restart()
            return true
        }
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (attackCooldown > 0) return true

            val midX = width / 2f
            val side = if (event.x < midX) "L" else "R"
            val now = System.currentTimeMillis()

            var attackType: AttackType
            val timeSinceLastTap = now - lastTapTime

            if (timeSinceLastTap < 400 && side == lastTapSide) {
                attackType = AttackType.SPECIAL
            } else if (side == "L") {
                attackType = AttackType.PUNCH
            } else {
                attackType = AttackType.KICK
            }

            lastTapTime = now
            lastTapSide = side

            applyAttack(attackType, event.x, event.y)
            return true
        }
        return false
    }

    private fun applyAttack(type: AttackType, tapX: Float, tapY: Float) {
        attackCooldown = if (type == AttackType.SPECIAL) 30 else 18

        attackHistory.add(FilmFrame(type, System.currentTimeMillis()))
        if (attackHistory.size > 10) attackHistory.removeAt(0)

        var damage = type.damage.toFloat()

        if (comboMultiplier > 1f) {
            damage *= comboMultiplier
        }

        val recent = attackHistory.takeLast(3)
        if (recent.size >= 2) {
            val a = recent[recent.size - 2].type
            val b = recent[recent.size - 1].type
            if (a == AttackType.PUNCH && b == AttackType.KICK) {
                damage *= 1.5f
                bonusText = "ACTION-WESTERN: REVENGE FLICK!"
                bonusTextTimer = 40
                score += 25
                comboMultiplier = 1.5f
                comboMultiplierTimer = 60
            } else if (a == AttackType.KICK && b == AttackType.PUNCH) {
                damage *= 1.3f
                bonusText = "WESTERN-ACTION: BRAWL FILM!"
                bonusTextTimer = 40
                score += 20
                comboMultiplier = 1.3f
                comboMultiplierTimer = 50
            } else if (type == AttackType.SPECIAL) {
                if (a == AttackType.PUNCH) {
                    damage *= 1.6f
                    bonusText = "ACTION-SCIFI: CYBERPUNK!"
                    bonusTextTimer = 40
                    score += 30
                    comboMultiplier = 1.4f
                    comboMultiplierTimer = 60
                } else if (a == AttackType.KICK) {
                    damage *= 1.6f
                    bonusText = "WESTERN-SCIFI: SPACE COWBOY!"
                    bonusTextTimer = 40
                    score += 30
                    comboMultiplier = 1.4f
                    comboMultiplierTimer = 60
                }
            }
        }

        val uniqueRecent = recent.map { it.type }.distinct()
        if (uniqueRecent.size >= 3) {
            damage *= 2.0f
            bonusText = "FILM SEQUENCE! x2 DMG"
            bonusTextTimer = 45
            score += 50
            comboMultiplier = max(comboMultiplier, 2.0f)
            comboMultiplierTimer = 60
        }

        currentCombo = attackHistory.takeLast(5).joinToString("-") { it.type.label.take(1) }

        enemyHealth -= damage
        score += damage.toInt()
        shakeAmount = 8f

        val midY = height / 2f
        attackEffects.add(AttackEffect(
            tapX.coerceIn(40f, width - 40f),
            tapY.coerceIn(60f, height - 60f),
            type
        ))

        spawnGlitchBurst()

        if (enemyHealth <= 0) {
            enemyHealth = 0f
            gameOver = true
            gameOverWon = true
            score += 100
        }
    }

    private fun spawnGlitchBurst() {
        for (i in 0..15) {
            glitchParticles.add(GlitchParticle(
                width * 0.7f + random.nextFloat() * width * 0.15f,
                random.nextFloat() * height,
                (random.nextFloat() - 0.5f) * 8f,
                (random.nextFloat() - 0.5f) * 8f,
                20 + random.nextInt(20), 40
            ))
        }
    }

    private fun spawnFrameParticles(x: Float, y: Float, color: Int) {
        for (i in 0..10) {
            val angle = random.nextFloat() * PI.toFloat() * 2f
            val speed = random.nextFloat() * 5f + 2f
            frameParticles.add(Pair(
                x + cos(angle) * speed,
                y + sin(angle) * speed
            ))
        }
    }

    private val gameLoop = object : Runnable {
        override fun run() {
            if (gameOver) {
                invalidate()
                postDelayed(this, 33)
                return
            }
            update()
            invalidate()
            postDelayed(this, 33)
        }
    }

    init {
        post(gameLoop)
    }

    private fun update() {
        if (attackCooldown > 0) attackCooldown--
        if (bonusTextTimer > 0) bonusTextTimer--
        else bonusText = ""
        if (comboMultiplierTimer > 0) comboMultiplierTimer--
        else {
            comboMultiplier = 1f
            currentCombo = ""
        }

        if (shakeAmount > 0) shakeAmount *= 0.8f
        if (shakeAmount < 0.3f) shakeAmount = 0f

        enemyGlitchTimer++
        if (enemyGlitchTimer % 3 == 0) {
            enemyGlitchOffset = (random.nextFloat() - 0.5f) * 12f
        }

        enemyAttackTimer++
        if (enemyAttackTimer >= 90) {
            enemyAttackTimer = 0
            val dmg = 5f + random.nextFloat() * 5f
            playerHealth -= dmg
            shakeAmount = max(shakeAmount, 4f)
            spawnGlitchBurst()
            if (playerHealth <= 0) {
                playerHealth = 0f
                gameOver = true
                gameOverWon = false
            }
        }

        val effectIter = attackEffects.iterator()
        while (effectIter.hasNext()) {
            val e = effectIter.next()
            e.elapsed += 33
            if (e.elapsed > e.duration) effectIter.remove()
        }

        val glitchIter = glitchParticles.iterator()
        while (glitchIter.hasNext()) {
            val p = glitchIter.next()
            p.x += p.vx
            p.y += p.vy
            p.vx *= 0.96f
            p.vy *= 0.96f
            p.life--
            if (p.life <= 0) glitchIter.remove()
        }

        val fpIter = frameParticles.iterator()
        while (fpIter.hasNext()) {
            fpIter.remove()
        }

        hudCallback(score, if (comboMultiplierTimer > 0) "${currentCombo} x${comboMultiplier}" else "")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        var offsetX = 0f
        var offsetY = 0f
        if (shakeAmount > 0) {
            offsetX = (random.nextFloat() - 0.5f) * shakeAmount * 2f
            offsetY = (random.nextFloat() - 0.5f) * shakeAmount * 2f
        }

        canvas.save()
        canvas.translate(offsetX, offsetY)

        drawFilmGrid(canvas)
        drawEnemy(canvas)
        drawPlayer(canvas)
        drawHealthBars(canvas)
        drawAttackEffects(canvas)
        drawBonusText(canvas)
        drawGlitchParticles(canvas)

        canvas.restore()

        if (gameOver) {
            val overlay = Paint().apply { color = 0xBB000000.toInt(); style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlay)
            val textPaint = Paint().apply {
                color = if (gameOverWon) 0xFFFFDD00.toInt() else 0xFFFF3344.toInt()
                textSize = 48f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            val msg = if (gameOverWon) "CUT! YOU WIN!" else "CORRUPTED!"
            canvas.drawText(msg, width / 2f, height / 2f - 40, textPaint)
            textPaint.textSize = 24f
            textPaint.color = 0xFFCCCCCC.toInt()
            canvas.drawText("Score: $score", width / 2f, height / 2f + 10, textPaint)
            textPaint.textSize = 22f
            textPaint.color = 0xFFFFDD00.toInt()
            canvas.drawText("Tap to Re-Roll", width / 2f, height / 2f + 50, textPaint)
        }
    }

    private fun drawFilmGrid(canvas: Canvas) {
        val spacing = 40f
        for (x in 0..width.toInt() step spacing.toInt()) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), dividerPaint)
        }
        for (y in 0..height.toInt() step spacing.toInt()) {
            canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), dividerPaint)
        }
    }

    private fun drawPlayer(canvas: Canvas) {
        val cx = width * 0.2f
        val cy = height * 0.55f
        val reelRadius = width * 0.085f
        val holeRadius = reelRadius * 0.12f

        canvas.drawCircle(cx, cy, reelRadius + 4f, playerReelPaint)

        playerReelPaint.strokeWidth = 2f
        for (i in 0 until 6) {
            val angle = Math.toRadians((i * 60.0 + System.currentTimeMillis() * 0.02 % 360).toFloat())
            val sx = cx + cos(angle).toFloat() * (reelRadius - 2f)
            val sy = cy + sin(angle).toFloat() * (reelRadius - 2f)
            val ex = cx + cos(angle).toFloat() * reelRadius
            val ey = cy + sin(angle).toFloat() * reelRadius
            canvas.drawLine(sx, sy, ex, ey, playerReelPaint)
        }
        playerReelPaint.strokeWidth = 4f

        canvas.drawCircle(cx, cy, reelRadius * 0.5f, playerCorePaint)
        canvas.drawCircle(cx, cy, holeRadius, Paint().apply {
            color = 0xFF0A0A1A.toInt()
            style = Paint.Style.FILL
        })

        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45.0).toFloat())
            val dist = reelRadius * 0.75f
            val spx = cx + cos(angle).toFloat() * (dist - 3f)
            val spy = cy + sin(angle).toFloat() * (dist - 3f)
            canvas.drawCircle(spx, spy, 2f, playerSprocketPaint)
        }
    }

    private fun drawEnemy(canvas: Canvas) {
        val cx = width * 0.72f
        val cy = height * 0.5f
        val size = width * 0.12f

        val glitchOffX = if (enemyGlitchTimer % 7 < 3) enemyGlitchOffset else 0f
        val glitchOffY = if (enemyGlitchTimer % 11 < 4) enemyGlitchOffset * 0.5f else 0f

        for (i in 0 until 3) {
            val alpha = (60 + i * 20).coerceIn(0, 255)
            val offset = i * 4f
            enemyPaint.alpha = alpha
            val cornerRadius = size * 0.15f
            canvas.drawRoundRect(
                cx - size + offset + glitchOffX,
                cy - size * 0.7f + offset + glitchOffY,
                cx + size - offset + glitchOffX,
                cy + size * 0.7f - offset + glitchOffY,
                cornerRadius, cornerRadius,
                enemyPaint
            )
        }
        enemyPaint.alpha = 255

        canvas.drawRoundRect(
            cx - size, cy - size * 0.7f,
            cx + size, cy + size * 0.7f,
            size * 0.15f, size * 0.15f,
            enemyPaint
        )

        val innerH = size * 0.4f
        canvas.drawRect(
            cx - size * 0.6f, cy - innerH,
            cx + size * 0.6f, cy + innerH,
            enemyCorePaint
        )

        val barCount = 5
        val barWidth = size * 0.12f
        val barGap = size * 0.18f
        val totalBarWidth = barCount * barWidth + (barCount - 1) * barGap
        val barStartX = cx - totalBarWidth / 2f
        val barY = cy + innerH + size * 0.15f

        for (i in 0 until barCount) {
            val barAlpha = (100 + (enemyGlitchTimer + i * 7) % 155).coerceIn(0, 255)
            glitchPaint.alpha = barAlpha
            canvas.drawRect(
                barStartX + i * (barWidth + barGap),
                barY,
                barStartX + i * (barWidth + barGap) + barWidth,
                barY + 6f,
                glitchPaint
            )
        }
        glitchPaint.alpha = 0x44
        enemyPaint.alpha = 255

        val flickerAlpha = (120 + sin(enemyGlitchTimer * 0.3f) * 80).toInt().coerceIn(40, 220)
        val scanPaint = Paint().apply {
            color = (flickerAlpha shl 24) or 0x00FF1144.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(
            cx - size, cy - size * 0.2f,
            cx + size, cy + size * 0.2f,
            scanPaint
        )
    }

    private fun drawHealthBars(canvas: Canvas) {
        val barWidth = width * 0.65f
        val barHeight = 18f
        val barX = width * 0.025f

        val playerBarY = 12f
        val frameW = barWidth / 8f

        for (i in 0 until 8) {
            val segmentHealth = MAX_HEALTH / 8f
            val segmentThreshold = (i + 1) * segmentHealth
            val paint = if (playerHealth >= segmentThreshold) filmStripPaint else filmStripLostPaint
            canvas.drawRect(
                barX + i * frameW + 2f,
                playerBarY + 2f,
                barX + (i + 1) * frameW - 2f,
                playerBarY + barHeight - 2f,
                paint
            )
            canvas.drawRect(
                barX + i * frameW + 2f,
                playerBarY + 2f,
                barX + (i + 1) * frameW - 2f,
                playerBarY + barHeight - 2f,
                filmBorderPaint
            )
            canvas.drawCircle(barX + i * frameW + frameW / 2f, playerBarY, 3f, playerSprocketPaint)
            canvas.drawCircle(barX + i * frameW + frameW / 2f, playerBarY + barHeight, 3f, playerSprocketPaint)
        }

        canvas.drawRect(barX, playerBarY, barX + barWidth, playerBarY + barHeight, filmBorderPaint)
        canvas.drawCircle(barX, playerBarY + barHeight / 2f, 4f, playerSprocketPaint)
        canvas.drawCircle(barX + barWidth, playerBarY + barHeight / 2f, 4f, playerSprocketPaint)

        val labelPaint = Paint().apply {
            color = 0xFFFFDD00.toInt()
            textSize = 12f
            typeface = Typeface.MONOSPACE
        }
        canvas.drawText("REEL HEALTH", barX, playerBarY - 4f, labelPaint)

        val enemyBarY = height - barHeight - 12f
        val ebx = barX
        val ebw = barWidth * (enemyHealth / ENEMY_MAX_HEALTH)
        val enemyBarPaint = Paint().apply {
            color = 0xFFFF3366.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(ebx, enemyBarY, ebx + ebw, enemyBarY + barHeight, enemyBarPaint)
        canvas.drawRect(ebx, enemyBarY, ebx + barWidth, enemyBarY + barHeight, filmBorderPaint)

        for (i in 1 until 5) {
            val lx = ebx + barWidth * i / 5f
            canvas.drawLine(lx, enemyBarY, lx, enemyBarY + barHeight, Paint().apply {
                color = 0x440A0A1A.toInt()
                strokeWidth = 1f
            })
        }

        val elabelPaint = Paint().apply {
            color = 0xFFFF3366.toInt()
            textSize = 12f
            typeface = Typeface.MONOSPACE
        }
        canvas.drawText("CORRUPTED DATA", ebx, enemyBarY - 4f, elabelPaint)
    }

    private fun drawAttackEffects(canvas: Canvas) {
        for (effect in attackEffects) {
            val progress = effect.elapsed / effect.duration.toFloat()
            val alpha = ((1f - progress) * 255).toInt().coerceIn(0, 255)

            attackTextPaint.color = (alpha shl 24) or effect.type.color
            attackTextPaint.textSize = 28f + progress * 16f

            canvas.drawText(
                effect.type.label,
                effect.x,
                effect.y - progress * 30f,
                attackTextPaint
            )

            val genrePaint = Paint().apply {
                color = (alpha shl 24) or 0xFFFFDD00.toInt()
                textSize = 16f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.MONOSPACE
                isAntiAlias = true
            }
            canvas.drawText(
                effect.type.genre,
                effect.x,
                effect.y - progress * 30f + 24f,
                genrePaint
            )
        }
    }

    private fun drawBonusText(canvas: Canvas) {
        if (bonusTextTimer > 0) {
            val alpha = (min(bonusTextTimer.toFloat() / 15f, 1f) * min((40 - bonusTextTimer).toFloat() / 5f, 1f) * 255).toInt().coerceIn(0, 255)
            bonusTextPaint.color = (alpha shl 24) or 0xFFFF8800.toInt()
            canvas.drawText(bonusText, width / 2f, height * 0.35f, bonusTextPaint)
        }
    }

    private fun drawGlitchParticles(canvas: Canvas) {
        for (p in glitchParticles) {
            val alpha = (p.life.toFloat() / p.maxLife * 255).toInt().coerceIn(0, 255)
            particlePaint.color = (alpha shl 24) or 0xFFFF1144.toInt()
            canvas.drawRect(p.x - 2f, p.y - 1f, p.x + 2f, p.y + 1f, particlePaint)
            if (random.nextFloat() > 0.3f) {
                particlePaint.color = (alpha shl 24) or 0xFF44AAFF.toInt()
                canvas.drawRect(p.x + 6f, p.y - 2f, p.x + 8f, p.y, particlePaint)
            }
        }

        for ((fx, fy) in frameParticles) {
            particlePaint.color = 0x88FFDD00.toInt()
            canvas.drawCircle(fx, fy, 2f, particlePaint)
        }
    }

    fun restart() {
        playerHealth = MAX_HEALTH
        enemyHealth = ENEMY_MAX_HEALTH
        score = 0
        gameOver = false
        gameOverWon = false
        attackCooldown = 0
        enemyAttackTimer = 0
        lastTapTime = 0L
        lastTapSide = ""
        currentCombo = ""
        attackHistory.clear()
        attackEffects.clear()
        glitchParticles.clear()
        frameParticles.clear()
        shakeAmount = 0f
        enemyGlitchOffset = 0f
        bonusText = ""
        bonusTextTimer = 0
        comboMultiplier = 1f
        comboMultiplierTimer = 0
        spawnInitialGlitchParticles()
        hudCallback(0, "")
        invalidate()
    }
}
