package com.example.game

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.data.model.UserProfile
import com.example.data.repository.GameRepository
import com.example.game.audio.GameAudio
import kotlinx.coroutines.*
import kotlin.random.Random

enum class GameState {
    MENU, COUNTDOWN, RUNNING, PAUSED, GAME_OVER
}

enum class ObstacleType {
    WALL, // Block lane entirely
    LOW_BARRIER, // Jump over
    HIGH_BARRIER // Slide under
}

enum class PowerUpType {
    SHIELD, SPEED, MAGNET
}

data class Obstacle(
    val id: Long,
    val lane: Int, // -1 (left), 0 (center), 1 (right)
    val type: ObstacleType,
    var z: Float, // Depth from horizon (e.g. 120.0f to 0.0f)
    var isPassed: Boolean = false,
    var isDestroyed: Boolean = false
)

data class Coin(
    val id: Long,
    val lane: Int,
    var z: Float,
    var y: Float = 0f, // Floating height
    var isCollected: Boolean = false,
    var isAttracted: Boolean = false // Moving toward magnet
)

data class PowerUpItem(
    val id: Long,
    val lane: Int,
    var z: Float,
    val type: PowerUpType,
    var isCollected: Boolean = false
)

class GameEngine(
    private val repository: GameRepository,
    private val scope: CoroutineScope
) {
    // Game state parameters
    var activeState by mutableStateOf(GameState.MENU)
        private set

    var currentScore by mutableStateOf(0L)
        private set

    var currentCoinsCollected by mutableStateOf(0)
        private set

    var currentDistance by mutableStateOf(0f)
        private set

    var countdownValue by mutableStateOf(3)
        private set

    // Speed details
    var baseSpeed = 1.0f
    val currentSpeedMultiplier: Float
        get() = baseSpeed + (currentDistance / 1200f) + (if (speedBoostTicks > 0) 1.2f else 0.0f)

    // Player position coordinates
    var playerLanePosition by mutableStateOf(0.0f) // Animated left-to-right (-1f to 1f)
    var targetLane = 0 // Target lane (-1, 0, 1)
    
    // Jump details
    var playerY by mutableStateOf(0.0f) // Jumps (0.0f to max height)
    private var jumpVelocity = 0.0f
    private val gravity = 0.12f

    // Slide details
    var slideTicksRemaining by mutableStateOf(0)
        private set

    // Active powerup timers (ticks remaining)
    var shieldTicks by mutableStateOf(0)
        private set
    var speedBoostTicks by mutableStateOf(0)
        private set
    var magnetTicks by mutableStateOf(0)
        private set

    // Maximum durations based on upgrade levels (ticks @ 60 FPS aprox 1/60s)
    private var basePowerupDuration = 300 // 5 seconds
    private var upgradeMultiplier = 90 // +1.5 seconds per upgrade

    // Active entities in simulation
    val obstacles = mutableStateListOf<Obstacle>()
    val coins = mutableStateListOf<Coin>()
    val powerUps = mutableStateListOf<PowerUpItem>()

    // Local profile reference
    private var activeProfile: UserProfile = UserProfile()
    private var entityIdCounter = 0L

    // Spawning timers
    private var obstacleSpawnTimer = 0
    private var coinSpawnTimer = 0
    private var powerupSpawnTimer = 0

    // Main Game Loop Thread
    private var gameLoopJob: Job? = null

    init {
        // Collect user profile settings securely
        scope.launch {
            repository.userProfileFlow.collect { profile ->
                activeProfile = profile
            }
        }
    }

    // Lane Navigation Inputs
    fun swipeLeft() {
        if (activeState != GameState.RUNNING) return
        if (targetLane > -1) {
            targetLane--
            GameAudio.playClick()
        }
    }

    fun swipeRight() {
        if (activeState != GameState.RUNNING) return
        if (targetLane < 1) {
            targetLane++
            GameAudio.playClick()
        }
    }

    fun jump() {
        if (activeState != GameState.RUNNING) return
        if (playerY == 0.0f && slideTicksRemaining == 0) {
            playerY = 0.1f
            jumpVelocity = 1.9f
            GameAudio.playClick()
        }
    }

    fun slide() {
        if (activeState != GameState.RUNNING) return
        if (playerY == 0.0f) {
            slideTicksRemaining = 60 // ~1 second sliding
            GameAudio.playClick()
        }
    }

    fun startGame() {
        // Prepare assets
        obstacles.clear()
        coins.clear()
        powerUps.clear()
        currentScore = 0
        currentCoinsCollected = 0
        currentDistance = 0f
        targetLane = 0
        playerLanePosition = 0.0f
        playerY = 0.0f
        jumpVelocity = 0.0f
        slideTicksRemaining = 0
        shieldTicks = 0
        speedBoostTicks = 0
        magnetTicks = 0
        entityIdCounter = 0L
        obstacleSpawnTimer = 0
        coinSpawnTimer = 0
        powerupSpawnTimer = 100

        activeState = GameState.COUNTDOWN
        countdownValue = 3

        scope.launch {
            while (countdownValue > 0) {
                GameAudio.playClick()
                delay(1000)
                countdownValue--
            }
            activeState = GameState.RUNNING
            runGameLoop()
        }
    }

    fun pauseGame() {
        if (activeState == GameState.RUNNING) {
            activeState = GameState.PAUSED
            gameLoopJob?.cancel()
        }
    }

    fun resumeGame() {
        if (activeState == GameState.PAUSED) {
            activeState = GameState.RUNNING
            runGameLoop()
        }
    }

    fun exitToMenu() {
        gameLoopJob?.cancel()
        activeState = GameState.MENU
    }

    // Main Game Loop executing calculations at 60Hz
    private fun runGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = scope.launch(Dispatchers.Default) {
            val frameTimeMs = 16L
            while (activeState == GameState.RUNNING) {
                val start = System.currentTimeMillis()
                updateGameTick()
                val elapsed = System.currentTimeMillis() - start
                val delayTime = maxOf(1L, frameTimeMs - elapsed)
                delay(delayTime)
            }
        }
    }

    @Synchronized
    private fun updateGameTick() {
        // 1. Smoothly slide player towards target lane in X space
        playerLanePosition += (targetLane - playerLanePosition) * 0.22f

        // 2. Process vertical jumping physics (gravity model)
        if (playerY > 0.0f) {
            playerY += jumpVelocity
            jumpVelocity -= gravity
            if (playerY <= 0.0f) {
                playerY = 0.0f
                jumpVelocity = 0.0f
            }
        }

        // 3. Update active ticks for buffers
        if (slideTicksRemaining > 0) slideTicksRemaining--
        if (shieldTicks > 0) shieldTicks--
        if (speedBoostTicks > 0) speedBoostTicks--
        if (magnetTicks > 0) magnetTicks--

        // 4. Update speed & points multipliers
        val multiplier = (1 + (currentDistance / 300f).toInt()) * (if (speedBoostTicks > 0) 2 else 1)
        currentDistance += 0.25f * currentSpeedMultiplier
        currentScore += multiplier

        // 5. Simulate entities moving towards the screen
        val scrollSpeed = 0.85f * currentSpeedMultiplier
        
        // Update obstacles
        val obstaclesToRemove = mutableListOf<Obstacle>()
        for (obstacle in obstacles) {
            obstacle.z -= scrollSpeed

            // Collision check depth
            val collisionDepthMin = 8.5f
            val collisionDepthMax = 12.5f

            if (obstacle.z in collisionDepthMin..collisionDepthMax && !obstacle.isPassed && !obstacle.isDestroyed) {
                // Determine horizontal lane intersection
                val isPlayerInLane = Math.abs(playerLanePosition - obstacle.lane) < 0.65f
                if (isPlayerInLane) {
                    var avoidsCollision = false
                    when (obstacle.type) {
                        ObstacleType.LOW_BARRIER -> {
                            if (playerY > 2.0f) avoidsCollision = true
                        }
                        ObstacleType.HIGH_BARRIER -> {
                            if (slideTicksRemaining > 0) avoidsCollision = true
                        }
                        ObstacleType.WALL -> {
                            // Instant collision unless shield active
                        }
                    }

                    if (!avoidsCollision) {
                        if (shieldTicks > 0) {
                            shieldTicks = 0 // Break shield
                            obstacle.isDestroyed = true
                            GameAudio.playShieldBreak()
                        } else {
                            // GAME OVER CRASH!
                            gameOverCrash()
                            return
                        }
                    }
                }
            }

            if (obstacle.z < 2f) {
                if (!obstacle.isPassed && !obstacle.isDestroyed) {
                    obstacle.isPassed = true
                }
            }

            // Remove expired offscreen items
            if (obstacle.z < 0.5f) {
                obstaclesToRemove.add(obstacle)
            }
        }
        obstacles.removeAll(obstaclesToRemove)

        // Update coins
        val coinsToRemove = mutableListOf<Coin>()
        for (coin in coins) {
            // Magnet logic
            if (magnetTicks > 0 && coin.z < 45f) {
                coin.isAttracted = true
                // Fly smoothly towards player lane and screen
                coin.z += (10f - coin.z) * 0.15f
                val targetX = playerLanePosition
                val diffX = targetX - coin.lane
                // Move lane fractionally
                val attractionFactor = 0.2f
                val deltaZ = coin.z - 10f
                if (Math.abs(deltaZ) < 2f) {
                    coin.z = 10f
                }
            } else {
                coin.z -= scrollSpeed
            }

            var removed = false
            // Collection Check
            if (coin.z in 8f..14f && !coin.isCollected) {
                val isNearPlayer = Math.abs(playerLanePosition - coin.lane) < 0.75f && playerY < 6.0f
                if (isNearPlayer || (magnetTicks > 0 && coin.isAttracted)) {
                    coin.isCollected = true
                    currentCoinsCollected++
                    GameAudio.playCoinCollected()
                    coinsToRemove.add(coin)
                    removed = true
                }
            }

            if (!removed && coin.z < 0.5f) {
                coinsToRemove.add(coin)
            }
        }
        coins.removeAll(coinsToRemove)

        // Update Powerups on floor
        val powerupsToRemove = mutableListOf<PowerUpItem>()
        for (powerUp in powerUps) {
            powerUp.z -= scrollSpeed

            var removed = false
            if (powerUp.z in 8f..14f && !powerUp.isCollected) {
                val isNearPlayer = Math.abs(playerLanePosition - powerUp.lane) < 0.75f && playerY < 6.0f
                if (isNearPlayer) {
                    powerUp.isCollected = true
                    activatePowerup(powerUp.type)
                    powerActsCounterTrigger()
                    powerupsToRemove.add(powerUp)
                    removed = true
                }
            }

            if (!removed && powerUp.z < 0.5f) {
                powerupsToRemove.add(powerUp)
            }
        }
        powerUps.removeAll(powerupsToRemove)

        // 6. Handle Spawns
        obstacleSpawnTimer++
        val minObstacleTicks = maxOf(35, 120 - (currentDistance / 10f).toInt())
        if (obstacleSpawnTimer > minObstacleTicks + Random.nextInt(0, 40)) {
            spawnObstacle()
            obstacleSpawnTimer = 0
        }

        coinSpawnTimer++
        if (coinSpawnTimer > 25) {
            spawnCoinRow()
            coinSpawnTimer = 0
        }

        powerupSpawnTimer++
        if (powerupSpawnTimer > 350 + Random.nextInt(0, 150)) {
            spawnPowerUp()
            powerupSpawnTimer = 0
        }
    }

    private fun spawnObstacle() {
        entityIdCounter++
        val lane = Random.nextInt(-1, 2)
        val type = when (Random.nextInt(0, 3)) {
            0 -> ObstacleType.LOW_BARRIER
            1 -> ObstacleType.HIGH_BARRIER
            else -> ObstacleType.WALL
        }
        obstacles.add(Obstacle(entityIdCounter, lane, type, 120f))
    }

    private fun spawnCoinRow() {
        entityIdCounter++
        val lane = Random.nextInt(-1, 2)
        val baseZ = 120f
        // Spawn 3 consecutive coins in a nice lane row
        for (i in 0 until 3) {
            coins.add(Coin(entityIdCounter + i, lane, baseZ + (i * 8f), y = 0.5f))
        }
        entityIdCounter += 3
    }

    private fun spawnPowerUp() {
        entityIdCounter++
        val lane = Random.nextInt(-1, 2)
        val type = when (Random.nextInt(0, 3)) {
            0 -> PowerUpType.SHIELD
            1 -> PowerUpType.SPEED
            else -> PowerUpType.MAGNET
        }
        powerUps.add(PowerUpItem(entityIdCounter, lane, 120f, type))
    }

    private fun activatePowerup(type: PowerUpType) {
        GameAudio.playPowerUp()
        when (type) {
            PowerUpType.SHIELD -> {
                val duration = basePowerupDuration + (activeProfile.shieldLevel - 1) * upgradeMultiplier
                shieldTicks = duration
            }
            PowerUpType.SPEED -> {
                val duration = basePowerupDuration + (activeProfile.speedLevel - 1) * upgradeMultiplier
                speedBoostTicks = duration
            }
            PowerUpType.MAGNET -> {
                val duration = basePowerupDuration + (activeProfile.magnetLevel - 1) * upgradeMultiplier
                magnetTicks = duration
            }
        }
    }

    private fun powerActsCounterTrigger() {
        scope.launch {
            repository.incrementPowerUpsPower()
        }
    }

    private fun gameOverCrash() {
        activeState = GameState.GAME_OVER
        gameLoopJob?.cancel()
        GameAudio.playCrash()

        // Submit to database
        scope.launch {
            repository.submitGameRun(currentScore, currentCoinsCollected)
        }
    }

    // Triggered if rewarded ad claims score restoration
    fun revivePlayer() {
        if (activeState == GameState.GAME_OVER) {
            // Restore play
            obstacles.clear()
            coins.clear()
            powerUps.clear()
            shieldTicks = 120 // Brief invul shield
            activeState = GameState.RUNNING
            runGameLoop()
            GameAudio.playPowerUp()
        }
    }
}
