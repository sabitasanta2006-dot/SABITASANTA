package com.example.game

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.example.audio.GameAudioSynthesizer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class GameEngine(
    val characterConfig: CharacterConfig,
    private val audioSynthesizer: GameAudioSynthesizer,
    private val onGameOver: (score: Int, coins: Int, distance: Int) -> Unit
) {
    companion object {
        const val VIRTUAL_WIDTH = 1920f
        const val VIRTUAL_HEIGHT = 1080f
        const val FLOOR_Y = 880f
        const val RUNNER_X = 250f
        const val RUNNER_WIDTH = 100f
        const val RUNNER_HEIGHT = 120f
        const val SLIDE_HEIGHT = 60f
    }

    // Mutable state flows for UI rendering
    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _coinsCollected = MutableStateFlow(0)
    val coinsCollected: StateFlow<Int> = _coinsCollected.asStateFlow()

    private val _distance = MutableStateFlow(0)
    val distance: StateFlow<Int> = _distance.asStateFlow()

    private val _health = MutableStateFlow(3) // Starts with 3 lives
    val health: StateFlow<Int> = _health.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    // Game items
    val coins = mutableListOf<Coin>()
    val obstacles = mutableListOf<Obstacle>()
    val powerUps = mutableListOf<PowerUpItem>()
    val particles = mutableListOf<Particle>()

    // Power-up durations (in frames, ~60 FPS)
    val activePowerUps = mutableMapOf<PowerUpType, Int>()

    // Physics state
    var runnerY = FLOOR_Y - RUNNER_HEIGHT
    var runnerVelocityY = 0f
    var runnerState = CharacterState.RUNNING
    var slideFrameCount = 0
    var isInvulnerable = false
    var invulnerableFrames = 0

    // Stats
    var rawDistance = 0f
    var baseGameSpeed = 12f
    var currentSpeed = 12f
    var gameTime = 0L

    // Parallax background offsets
    var bgOffsetFar = 0f
    var bgOffsetMid = 0f
    var bgOffsetClose = 0f

    // Day Night Cycle
    var dayNightCycle = 0f // 0.0 to 1.0 (0.0=noon, 0.25=sunset, 0.5=night, 0.75=sunrise)

    private var gameLoopJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun start() {
        if (_isPlaying.value) return
        reset()
        _isPlaying.value = true
        _isPaused.value = false
        gameLoopJob = scope.launch {
            var lastTime = System.currentTimeMillis()
            while (isActive && _isPlaying.value) {
                if (!_isPaused.value) {
                    val now = System.currentTimeMillis()
                    update()
                    lastTime = now
                }
                delay(16) // Target ~60 FPS
            }
        }
    }

    fun pause() {
        if (_isPlaying.value) {
            _isPaused.value = !_isPaused.value
        }
    }

    fun stop() {
        _isPlaying.value = false
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    private fun reset() {
        _score.value = 0
        _coinsCollected.value = 0
        _distance.value = 0
        _health.value = 3
        coins.clear()
        obstacles.clear()
        powerUps.clear()
        particles.clear()
        activePowerUps.clear()

        runnerY = FLOOR_Y - RUNNER_HEIGHT
        runnerVelocityY = 0f
        runnerState = CharacterState.RUNNING
        slideFrameCount = 0
        isInvulnerable = false
        invulnerableFrames = 0

        rawDistance = 0f
        baseGameSpeed = 12f
        currentSpeed = 12f
        gameTime = 0L
        dayNightCycle = 0f

        bgOffsetFar = 0f
        bgOffsetMid = 0f
        bgOffsetClose = 0f

        // Initial generation
        generateObstaclesAndCoins(spawnX = VIRTUAL_WIDTH + 200f)
    }

    // Input Actions
    fun jump() {
        if (_isPaused.value || runnerState == CharacterState.DEAD) return

        val aeroBonus = if (characterConfig.id == "aero_girl") 1.15f else 1.0f

        when (runnerState) {
            CharacterState.RUNNING, CharacterState.SLIDING -> {
                // Cancel slide
                if (runnerState == CharacterState.SLIDING) {
                    slideFrameCount = 0
                }
                runnerVelocityY = -28f * aeroBonus
                runnerState = CharacterState.JUMPING
                audioSynthesizer.playJump()
                spawnDustParticles(RUNNER_X + RUNNER_WIDTH / 2f, FLOOR_Y, 12)
            }
            CharacterState.JUMPING -> {
                runnerVelocityY = -26f * aeroBonus
                runnerState = CharacterState.DOUBLE_JUMPING
                audioSynthesizer.playDoubleJump()
                spawnSparkleParticles(RUNNER_X + RUNNER_WIDTH / 2f, runnerY + RUNNER_HEIGHT / 2f, characterConfig.primaryColor, 15)
            }
            else -> {}
        }
    }

    fun slide() {
        if (_isPaused.value || runnerState == CharacterState.DEAD) return
        
        if (runnerState == CharacterState.RUNNING) {
            runnerState = CharacterState.SLIDING
            slideFrameCount = if (characterConfig.id == "cyber_ninja") 65 else 45 // Longer slide for cyber ninja
            spawnDustParticles(RUNNER_X + RUNNER_WIDTH / 2f, FLOOR_Y, 6)
        }
    }

    // Game Update Loop
    private fun update() {
        gameTime++
        dayNightCycle = (gameTime % 7200) / 7200f // 7200 frames = ~2 mins full day/night cycle

        // 1. Difficulty & Speed adjustments
        if (gameTime % 300 == 0L) { // Every 5 seconds speed up
            baseGameSpeed = minOf(baseGameSpeed + 0.3f, 26f)
        }

        currentSpeed = if (activePowerUps.containsKey(PowerUpType.SPEED_BOOST)) {
            baseGameSpeed * 1.6f
        } else {
            baseGameSpeed
        }

        // 2. Parallax background offsets
        bgOffsetFar = (bgOffsetFar + currentSpeed * 0.1f) % VIRTUAL_WIDTH
        bgOffsetMid = (bgOffsetMid + currentSpeed * 0.3f) % VIRTUAL_WIDTH
        bgOffsetClose = (bgOffsetClose + currentSpeed * 0.6f) % VIRTUAL_WIDTH

        // 3. Update power-up durations
        val iterator = activePowerUps.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val framesLeft = entry.value - 1
            if (framesLeft <= 0) {
                iterator.remove()
            } else {
                entry.setValue(framesLeft)
            }
        }

        // 4. Invulnerability blinking frames
        if (invulnerableFrames > 0) {
            invulnerableFrames--
            if (invulnerableFrames == 0) {
                isInvulnerable = false
            }
        }

        // 5. Update Runner Physics
        updateRunner()

        // 6. Spawn new endless structures
        var maxItemX = 0f
        for (obs in obstacles) { if (obs.x > maxItemX) maxItemX = obs.x }
        for (coin in coins) { if (coin.x > maxItemX) maxItemX = coin.x }
        for (pu in powerUps) { if (pu.x > maxItemX) maxItemX = pu.x }

        if (maxItemX < VIRTUAL_WIDTH + 800f) {
            generateObstaclesAndCoins(spawnX = maxOf(VIRTUAL_WIDTH + 400f, maxItemX + Random.nextFloat() * 400f + 300f))
        }

        // 7. Update Obstacles, Coins, PowerUps & Particles
        updateGameItems()

        // 8. Distances and score
        rawDistance += currentSpeed / 60f // Meters
        _distance.value = rawDistance.toInt()
        
        val coinMultiplier = if (activePowerUps.containsKey(PowerUpType.DOUBLE_COINS)) 20 else 10
        _score.value = _distance.value + (_coinsCollected.value * coinMultiplier)
    }

    private fun updateRunner() {
        val gravityAcc = if (characterConfig.id == "aero_girl") 0.9f else 1.2f

        if (runnerState == CharacterState.JUMPING || runnerState == CharacterState.DOUBLE_JUMPING) {
            runnerY += runnerVelocityY
            runnerVelocityY += gravityAcc // Apply gravity

            val currentHeight = RUNNER_HEIGHT
            if (runnerY + currentHeight >= FLOOR_Y) {
                runnerY = FLOOR_Y - currentHeight
                runnerVelocityY = 0f
                runnerState = CharacterState.RUNNING
                spawnDustParticles(RUNNER_X + RUNNER_WIDTH / 2f, FLOOR_Y, 8)
            }
        } else if (runnerState == CharacterState.SLIDING) {
            slideFrameCount--
            if (slideFrameCount <= 0) {
                runnerState = CharacterState.RUNNING
            }
            // Dust trail while sliding
            if (gameTime % 4 == 0L) {
                spawnDustParticles(RUNNER_X, FLOOR_Y, 2)
            }
        } else if (runnerState == CharacterState.RUNNING) {
            // Dust trail while running
            if (gameTime % 6 == 0L) {
                spawnDustParticles(RUNNER_X, FLOOR_Y, 2)
            }
        }

        // Speed boost trail sparkles
        if (activePowerUps.containsKey(PowerUpType.SPEED_BOOST) && gameTime % 3 == 0L) {
            spawnSparkleParticles(RUNNER_X, runnerY + Random.nextFloat() * RUNNER_HEIGHT, Color(0xFFFF9800), 2)
        }
        
        // Character specific trail (cyber ninja blue trail)
        if (characterConfig.id == "cyber_ninja" && runnerState == CharacterState.SLIDING && gameTime % 2 == 0L) {
            spawnSparkleParticles(RUNNER_X, runnerY + SLIDE_HEIGHT / 2f, Color(0xFF00E5FF), 3)
        }
    }

    private fun updateGameItems() {
        // --- 1. Obstacles ---
        val obsIterator = obstacles.iterator()
        while (obsIterator.hasNext()) {
            val obs = obsIterator.next()
            obs.x -= currentSpeed
            obs.stateTime += 1f

            // Dynamic movement behaviors for specific obstacles
            when (obs.type) {
                ObstacleType.FLYING_BIRD -> {
                    // Flaps up and down
                    obs.y = obs.startY + sin(obs.stateTime * 0.1f) * 60f
                }
                ObstacleType.ROBOT -> {
                    // Moves left and right on the ground slightly
                    obs.x += obs.speedX
                }
                ObstacleType.GHOST -> {
                    // hovers ghost-like
                    obs.y = obs.startY + cos(obs.stateTime * 0.05f) * 40f
                    obs.x -= currentSpeed * 0.15f // ghost floats slightly faster
                }
                ObstacleType.FALLING_PLATFORM -> {
                    // Drops down when runner gets close
                    if (obs.x < RUNNER_X + 400f) {
                        obs.y += 6f
                    }
                }
                else -> {}
            }

            // Collision check
            if (!obs.isPassed && checkCollision(obs)) {
                handleDamage()
                obs.isPassed = true
            }

            // Remove off-screen
            if (obs.x + obs.width < -100f) {
                obsIterator.remove()
            }
        }

        // --- 2. Coins ---
        val coinIterator = coins.iterator()
        val magnetActive = activePowerUps.containsKey(PowerUpType.COIN_MAGNET)
        val doubleCoins = activePowerUps.containsKey(PowerUpType.DOUBLE_COINS)
        val hasMagnetAbilityBonus = characterConfig.id == "star_knight"

        while (coinIterator.hasNext()) {
            val coin = coinIterator.next()
            coin.x -= currentSpeed

            // Magnet attraction physics
            if (magnetActive && !coin.isCollected) {
                val runnerCenterX = RUNNER_X + RUNNER_WIDTH / 2f
                val runnerCenterY = runnerY + (if (runnerState == CharacterState.SLIDING) SLIDE_HEIGHT else RUNNER_HEIGHT) / 2f
                val dx = runnerCenterX - coin.x
                val dy = runnerCenterY - coin.y
                val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()

                // Star Knight has double range / strength
                val pullRadius = if (hasMagnetAbilityBonus) 600f else 400f
                if (dist < pullRadius) {
                    val strength = (1f - dist / pullRadius) * 25f
                    coin.x += (dx / dist) * strength
                    coin.y += (dy / dist) * strength
                }
            }

            // Check collision with player
            if (!coin.isCollected) {
                val runnerCenterX = RUNNER_X + RUNNER_WIDTH / 2f
                val runnerCenterY = runnerY + (if (runnerState == CharacterState.SLIDING) SLIDE_HEIGHT else RUNNER_HEIGHT) / 2f
                val dist = Math.hypot((coin.x - runnerCenterX).toDouble(), (coin.y - runnerCenterY).toDouble()).toFloat()
                
                val collectDist = if (runnerState == CharacterState.SLIDING) 70f else 90f
                if (dist < collectDist) {
                    coin.isCollected = true
                    val reward = if (doubleCoins || coin.isDouble) 2 else 1
                    _coinsCollected.value += reward
                    audioSynthesizer.playCoin()
                    spawnSparkleParticles(coin.x, coin.y, Color(0xFFFFD700), 8)
                }
            }

            // Remove off-screen or collected
            if (coin.isCollected || coin.x < -50f) {
                coinIterator.remove()
            }
        }

        // --- 3. Power-Ups ---
        val puIterator = powerUps.iterator()
        while (puIterator.hasNext()) {
            val pu = puIterator.next()
            pu.x -= currentSpeed

            // Check collision
            if (!pu.isCollected) {
                val runnerCenterX = RUNNER_X + RUNNER_WIDTH / 2f
                val runnerCenterY = runnerY + (if (runnerState == CharacterState.SLIDING) SLIDE_HEIGHT else RUNNER_HEIGHT) / 2f
                val dist = Math.hypot((pu.x - runnerCenterX).toDouble(), (pu.y - runnerCenterY).toDouble()).toFloat()
                
                val collectDist = if (runnerState == CharacterState.SLIDING) 75f else 95f
                if (dist < collectDist) {
                    pu.isCollected = true
                    handlePowerUpCollect(pu.type)
                    audioSynthesizer.playPowerUp()
                    spawnSparkleParticles(pu.x, pu.y, pu.type.color, 15)
                }
            }

            if (pu.isCollected || pu.x < -50f) {
                puIterator.remove()
            }
        }

        // --- 4. Particles ---
        val partIterator = particles.iterator()
        while (partIterator.hasNext()) {
            val p = partIterator.next()
            p.x += p.vx
            p.y += p.vy
            p.vy += p.gravity
            p.life -= p.decay

            if (p.life <= 0f) {
                partIterator.remove()
            }
        }
    }

    private fun checkCollision(obs: Obstacle): Boolean {
        // Calculate bounding box for player based on slide status
        val py = if (runnerState == CharacterState.SLIDING) runnerY + RUNNER_HEIGHT - SLIDE_HEIGHT else runnerY
        val ph = if (runnerState == CharacterState.SLIDING) SLIDE_HEIGHT else RUNNER_HEIGHT
        val px = RUNNER_X

        // Leave a slight padding for gameplay forgiveness (extremely common in runners)
        val padding = 15f
        return (px + padding < obs.x + obs.width - padding &&
                px + RUNNER_WIDTH - padding > obs.x + padding &&
                py + padding < obs.y + obs.height - padding &&
                py + ph - padding > obs.y + padding)
    }

    private fun handleDamage() {
        // Immune during speed boost or cyber ninja invuln slide
        if (activePowerUps.containsKey(PowerUpType.SPEED_BOOST)) return
        if (characterConfig.id == "cyber_ninja" && runnerState == CharacterState.SLIDING) return
        if (isInvulnerable) return

        // If shield is active, absorb damage
        if (activePowerUps.containsKey(PowerUpType.SHIELD)) {
            activePowerUps.remove(PowerUpType.SHIELD)
            audioSynthesizer.playPowerUp() // positive sound for deflecting
            triggerDamageBlink(90) // Temporary flashing
            spawnSparkleParticles(RUNNER_X + RUNNER_WIDTH / 2f, runnerY + RUNNER_HEIGHT / 2f, Color(0xFF03A9F4), 25)
            return
        }

        // Apply real damage
        audioSynthesizer.playDamage()
        spawnSparkleParticles(RUNNER_X + RUNNER_WIDTH / 2f, runnerY + RUNNER_HEIGHT / 2f, Color.Red, 30)
        _health.value = maxOf(0, _health.value - 1)

        if (_health.value <= 0) {
            handleDeath()
        } else {
            triggerDamageBlink(120) // 2 seconds of invuln blink
        }
    }

    private fun triggerDamageBlink(frames: Int) {
        isInvulnerable = true
        invulnerableFrames = frames
    }

    private fun handlePowerUpCollect(type: PowerUpType) {
        if (type == PowerUpType.EXTRA_LIFE) {
            _health.value = minOf(4, _health.value + 1) // Caps at 4 lives
        } else {
            // Apply standard powerups
            val doubleDurationBonus = if (characterConfig.id == "star_knight" && type == PowerUpType.COIN_MAGNET) 2 else 1
            val baseDuration = when (type) {
                PowerUpType.COIN_MAGNET -> 480 // 8 seconds
                PowerUpType.SPEED_BOOST -> 360 // 6 seconds
                PowerUpType.SHIELD -> 999999 // persists until hit
                PowerUpType.DOUBLE_COINS -> 480 // 8 seconds
                else -> 0
            }
            activePowerUps[type] = baseDuration * doubleDurationBonus
        }
    }

    private fun handleDeath() {
        runnerState = CharacterState.DEAD
        audioSynthesizer.playGameOver()
        stop()
        
        // Notify game over
        onGameOver(_score.value, _coinsCollected.value, _distance.value)
    }

    // --- Particle Spawners ---
    private fun spawnDustParticles(x: Float, y: Float, count: Int) {
        for (i in 0 until count) {
            particles.add(
                Particle(
                    x = x + Random.nextFloat() * 40f - 20f,
                    y = y - Random.nextFloat() * 10f,
                    vx = -currentSpeed * 0.3f - Random.nextFloat() * 2f,
                    vy = -Random.nextFloat() * 3f,
                    radius = Random.nextFloat() * 8f + 4f,
                    color = Color(0x3FFFFFFF), // Semi-transparent white
                    life = 1.0f,
                    decay = 0.03f + Random.nextFloat() * 0.02f,
                    gravity = -0.05f // Drift up slightly
                )
            )
        }
    }

    private fun spawnSparkleParticles(x: Float, y: Float, color: Color, count: Int) {
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2.0 * Math.PI
            val speed = Random.nextFloat() * 6f + 2f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    radius = Random.nextFloat() * 6f + 2f,
                    color = color.copy(alpha = 0.8f),
                    life = 1.0f,
                    decay = 0.02f + Random.nextFloat() * 0.03f,
                    gravity = 0.05f
                )
            )
        }
    }

    // --- Dynamic Level Generation Patterns ---
    private fun generateObstaclesAndCoins(spawnX: Float) {
        val rand = Random.nextInt(7)
        val id = UUID.randomUUID().toString()

        when (rand) {
            0 -> {
                // Static Spikes with simple arch of coins above it
                obstacles.add(Obstacle(id + "obs", spawnX, FLOOR_Y - 70f, 90f, 70f, ObstacleType.SPIKES))
                generateCoinArc(spawnX - 100f, FLOOR_Y - 250f, 5)
            }
            1 -> {
                // Flying bird hazard + sliding opportunity
                obstacles.add(Obstacle(id + "obs", spawnX, FLOOR_Y - 260f, 100f, 80f, ObstacleType.FLYING_BIRD, startY = FLOOR_Y - 260f))
                // Place a powerup under the bird
                if (Random.nextFloat() < 0.4f) {
                    generatePowerUp(spawnX + 10f, FLOOR_Y - 90f)
                } else {
                    coins.add(Coin(id + "c1", spawnX + 10f, FLOOR_Y - 60f))
                }
            }
            2 -> {
                // Rolling stone on the floor! Needs to be jumped over
                obstacles.add(Obstacle(id + "obs", spawnX, FLOOR_Y - 110f, 110f, 110f, ObstacleType.ROLLING_STONE))
                generateCoinArc(spawnX - 150f, FLOOR_Y - 280f, 6)
            }
            3 -> {
                // Floating platform structure with fire trap beneath it, powerup on top!
                obstacles.add(Obstacle(id + "p1", spawnX, FLOOR_Y - 300f, 250f, 40f, ObstacleType.FALLING_PLATFORM))
                obstacles.add(Obstacle(id + "trap", spawnX + 80f, FLOOR_Y - 80f, 90f, 80f, ObstacleType.FIRE_TRAP))
                
                // Add coins on top of the platform
                coins.add(Coin(id + "c1", spawnX + 50f, FLOOR_Y - 350f))
                coins.add(Coin(id + "c2", spawnX + 125f, FLOOR_Y - 350f, isDouble = true))
                coins.add(Coin(id + "c3", spawnX + 200f, FLOOR_Y - 350f))
            }
            4 -> {
                // Ghost hazard and double coins
                obstacles.add(Obstacle(id + "obs", spawnX, FLOOR_Y - 380f, 110f, 110f, ObstacleType.GHOST, startY = FLOOR_Y - 380f))
                coins.add(Coin(id + "dc1", spawnX - 200f, FLOOR_Y - 180f, isDouble = true))
                coins.add(Coin(id + "dc2", spawnX, FLOOR_Y - 180f, isDouble = true))
                coins.add(Coin(id + "dc3", spawnX + 200f, FLOOR_Y - 180f, isDouble = true))
            }
            5 -> {
                // Ground robot patrol (moves left/right)
                val robot = Obstacle(id + "obs", spawnX, FLOOR_Y - 130f, 100f, 130f, ObstacleType.ROBOT)
                robot.speedX = -2f // moves left independently
                obstacles.add(robot)
                generateCoinArc(spawnX - 100f, FLOOR_Y - 300f, 5)
            }
            6 -> {
                // Double spike trap with potential power-up reward in-between
                obstacles.add(Obstacle(id + "obs1", spawnX, FLOOR_Y - 70f, 90f, 70f, ObstacleType.SPIKES))
                obstacles.add(Obstacle(id + "obs2", spawnX + 350f, FLOOR_Y - 70f, 90f, 70f, ObstacleType.SPIKES))
                
                if (Random.nextFloat() < 0.6f) {
                    generatePowerUp(spawnX + 175f, FLOOR_Y - 260f)
                } else {
                    coins.add(Coin(id + "c1", spawnX + 175f, FLOOR_Y - 200f, isDouble = true))
                }
            }
        }
    }

    private fun generateCoinArc(startX: Float, peakY: Float, count: Int) {
        val id = UUID.randomUUID().toString()
        for (i in 0 until count) {
            val t = i.toDouble() / (count - 1)
            val x = startX + (i * 80f)
            // Parabolic arc formula
            val h = peakY
            val y = h + 150f * (4f * (t - 0.5) * (t - 0.5)).toFloat()
            coins.add(Coin(id + "c$i", x, y, isDouble = Random.nextFloat() < 0.2f))
        }
    }

    private fun generatePowerUp(x: Float, y: Float) {
        val type = PowerUpType.values().random()
        val id = UUID.randomUUID().toString()
        powerUps.add(PowerUpItem(id, x, y, type))
    }
}
