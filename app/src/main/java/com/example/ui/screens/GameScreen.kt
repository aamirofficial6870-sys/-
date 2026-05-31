package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.*
import com.example.game.audio.GameAudio
import kotlin.math.absoluteValue

@Composable
fun GameScreen(
    gameEngine: GameEngine,
    onNavigateBack: () -> Unit
) {
    val activeState = gameEngine.activeState
    val score = gameEngine.currentScore
    val coinsCount = gameEngine.currentCoinsCollected
    val distance = gameEngine.currentDistance

    // Drag gestures tracker
    var dragAccumulatorX by remember { mutableStateOf(0f) }
    var dragAccumulatorY by remember { mutableStateOf(0f) }
    val minSwipeDistance = 55f

    // Animated breathing loop for neon particles
    val infiniteTransition = rememberInfiniteTransition(label = "Glow")
    val ambientGlowVal by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowValue"
    )

    // Spinner for coin spinning
    val coinSpinVal by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CoinSpinner"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020108))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragAccumulatorX = 0f
                        dragAccumulatorY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulatorX += dragAmount.x
                        dragAccumulatorY += dragAmount.y
                    },
                    onDragEnd = {
                        val absX = dragAccumulatorX.absoluteValue
                        val absY = dragAccumulatorY.absoluteValue
                        if (absX > absY) {
                            if (absX > minSwipeDistance) {
                                if (dragAccumulatorX > 0) gameEngine.swipeRight()
                                else gameEngine.swipeLeft()
                            }
                        } else {
                            if (absY > minSwipeDistance) {
                                if (dragAccumulatorY < 0) gameEngine.jump()
                                else gameEngine.slide()
                            }
                        }
                    }
                )
            }
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // 1. Core 3D Rendering Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("game_rendering_canvas")
        ) {
            val w = size.width
            val h = size.height

            // 3D perspective variables
            val horizonY = h * 0.40f
            val centerX = w / 2f
            val roadBottomWidth = w * 0.95f
            val roadHorizonWidth = 14f

            // Clean radial sky lighting
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF13102C), Color(0xFF020108)),
                    center = Offset(centerX, horizonY),
                    radius = w * 0.7f
                )
            )

            // Draw Road surface path (Trapezoid in 2D perspective projection)
            val roadPath = Path().apply {
                moveTo(centerX - roadHorizonWidth / 2f, horizonY)
                lineTo(centerX + roadHorizonWidth / 2f, horizonY)
                lineTo(centerX + roadBottomWidth / 2f, h)
                lineTo(centerX - roadBottomWidth / 2f, h)
                close()
            }
            drawPath(
                path = roadPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF0E0B1B), Color(0xFF14112B)),
                    start = Offset(centerX, horizonY),
                    end = Offset(centerX, h)
                )
            )

            // Draw beautiful side neon barrier lines
            // Left boundary line
            drawLine(
                color = Color(0xFF00F0FF),
                start = Offset(centerX - roadHorizonWidth / 2f, horizonY),
                end = Offset(centerX - roadBottomWidth / 2f, h),
                strokeWidth = 3.dp.toPx()
            )
            // Right boundary line
            drawLine(
                color = Color(0xFFFF007F),
                start = Offset(centerX + roadHorizonWidth / 2f, horizonY),
                end = Offset(centerX + roadBottomWidth / 2f, h),
                strokeWidth = 3.dp.toPx()
            )

            // Perspective Lane Division grid lines
            val laneCount = 3
            // Vanishing point to bottom lane markers
            for (i in 1 until laneCount) {
                val fraction = i.toFloat() / laneCount
                val startX = centerX - (roadHorizonWidth / 2f) + (roadHorizonWidth * fraction)
                val endX = centerX - (roadBottomWidth / 2f) + (roadBottomWidth * fraction)
                drawLine(
                    color = Color(0xFF2C2556).copy(alpha = 0.55f),
                    start = Offset(startX, horizonY),
                    end = Offset(endX, h),
                    strokeWidth = 1.dp.toPx()                    
                )
            }

            // Draw scrolling horizontal grid bars representing distance speed progression
            val maxZ = 120f
            var gridLineZ = (distance % 25f)
            while (gridLineZ < maxZ) {
                // translate depth Z to vertical screen position with perspective compression
                val ratio = gridLineZ / maxZ // 0 at screen, 1 at horizon (or reverse for approach)
                val zProgress = 1f - ratio
                val lineY = horizonY + (zProgress * zProgress) * (h - horizonY)

                val gridW = roadHorizonWidth + (roadBottomWidth - roadHorizonWidth) * (zProgress * zProgress)
                drawLine(
                    color = Color(0xFF3B2F7E).copy(alpha = (1f - ratio) * 0.45f),
                    start = Offset(centerX - gridW / 2f, lineY),
                    end = Offset(centerX + gridW / 2f, lineY),
                    strokeWidth = 1.dp.toPx()
                )
                gridLineZ += 25f
            }

            // Projection math helper lambda
            // L is lane offset (-1f, 0f, 1f), Z is depth (120f to 0f)
            val projectX: (Float, Float) -> Float = { L, Z ->
                val ratio = 1f - (Z / maxZ)
                val scaleFactor = ratio * ratio // perspective compression
                val laneOffsetWidth = (roadBottomWidth - roadHorizonWidth) / 3f
                centerX + (L * laneOffsetWidth * scaleFactor)
            }

            val projectY: (Float, Float) -> Float = { _, Z ->
                val ratio = 1f - (Z / maxZ)
                horizonY + (ratio * ratio) * (h - horizonY)
            }

            val projectScale: (Float) -> Float = { Z ->
                val ratio = 1f - (Z / maxZ)
                // minimum 0.05 scaling, maximum 1.5 scaling
                0.03f + (ratio * ratio) * 1.35f
            }

            // 2. DRAW ENTITIES (Coins, Powerups, Obstacles) ordered by Z desc to maintain occlusion
            
            // Draw Coins
            gameEngine.coins.forEach { coin ->
                val cX = projectX(coin.lane.toFloat(), coin.z)
                val cY = projectY(coin.lane.toFloat(), coin.z) - (coin.y * 32.dp.toPx() * projectScale(coin.z))
                val scale = projectScale(coin.z)
                val coinRadius = 14f * scale

                if (coin.z in 1f..120f && !coin.isCollected) {
                    val angleOffset = coinSpinVal
                    // Yellow/Orange glowing spinning diamond shader
                    val gradient = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFEA33), Color(0xFFFF9900)),
                        center = Offset(cX, cY),
                        radius = coinRadius * 1.5f
                    )
                    
                    val coinPath = Path().apply {
                        moveTo(cX, cY - coinRadius * 1.4f)
                        lineTo(cX + coinRadius * 0.9f, cY)
                        lineTo(cX, cY + coinRadius * 1.4f)
                        lineTo(cX - coinRadius * 0.9f, cY)
                        close()
                    }
                    drawPath(coinPath, gradient)
                    drawPath(coinPath, Color.White, style = Stroke(width = maxOf(1f, 1.5f * scale)))
                }
            }

            // Draw Powerups on road floor
            gameEngine.powerUps.forEach { gear ->
                val gX = projectX(gear.lane.toFloat(), gear.z)
                val gY = projectY(gear.lane.toFloat(), gear.z)
                val scale = projectScale(gear.z)
                val gearSize = 16f * scale

                if (gear.z in 1f..120f && !gear.isCollected) {
                    val color = when (gear.type) {
                        PowerUpType.SHIELD -> Color(0xFF00F0FF)
                        PowerUpType.SPEED -> Color(0xFFFF007F)
                        PowerUpType.MAGNET -> Color(0xFF00FF66)
                    }
                    drawCircle(
                        color = color.copy(alpha = 0.3f),
                        radius = gearSize * 1.8f * ambientGlowVal,
                        center = Offset(gX, gY)
                    )
                    drawCircle(
                        color = color,
                        radius = gearSize,
                        center = Offset(gX, gY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = gearSize * 0.4f,
                        center = Offset(gX, gY)
                    )
                }
            }

            // Draw Obstacles
            gameEngine.obstacles.forEach { block ->
                val bX = projectX(block.lane.toFloat(), block.z)
                val bY = projectY(block.lane.toFloat(), block.z)
                val scale = projectScale(block.z)
                
                if (block.z in 1.5f..120f && !block.isDestroyed) {
                    val color = when (block.type) {
                        ObstacleType.WALL -> Color(0xFFFF2222)
                        ObstacleType.LOW_BARRIER -> Color(0xFFFF6600)
                        ObstacleType.HIGH_BARRIER -> Color(0xFFFF00AA)
                    }

                    // Render beautiful glowing 3D perspective boxes based on obstacle type
                    when (block.type) {
                        ObstacleType.WALL -> {
                            val boxW = 40f * scale
                            val boxH = 92f * scale
                            // Draw 3D pillar block
                            drawRoundRect(
                                color = color.copy(alpha = 0.28f),
                                topLeft = Offset(bX - boxW, bY - boxH),
                                size = Size(boxW * 2, boxH),
                                cornerRadius = CornerRadius(4f * scale, 4f * scale)
                            )
                            drawRoundRect(
                                color = color,
                                topLeft = Offset(bX - boxW + 2f, bY - boxH + 2f),
                                size = Size((boxW - 1) * 2, boxH - 4f),
                                style = Stroke(width = maxOf(1f, 3f * scale)),
                                cornerRadius = CornerRadius(4f * scale, 4f * scale)
                            )
                            // Draw raw light streak
                            drawLine(
                                color = Color.White,
                                start = Offset(bX - boxW / 2f, bY - boxH * 0.8f),
                                end = Offset(bX + boxW / 2f, bY - boxH * 0.8f),
                                strokeWidth = maxOf(0.5f, 2f * scale)
                            )
                        }
                        ObstacleType.LOW_BARRIER -> {
                            val barW = 36f * scale
                            val barH = 15f * scale
                            drawRoundRect(
                                color = color,
                                topLeft = Offset(bX - barW, bY - barH),
                                size = Size(barW * 2, barH),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                            drawRoundRect(
                                color = Color.White.copy(alpha = ambientGlowVal),
                                topLeft = Offset(bX - barW, bY - barH),
                                size = Size(barW * 2, barH),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                        ObstacleType.HIGH_BARRIER -> {
                            val barrierW = 38f * scale
                            val barrierH = 64f * scale
                            val clearH = 34f * scale
                            
                            // Draw realistic overhead gate with passage beneath
                            val gatePath = Path().apply {
                                moveTo(bX - barrierW, bY)
                                lineTo(bX - barrierW, bY - barrierH)
                                lineTo(bX + barrierW, bY - barrierH)
                                lineTo(bX + barrierW, bY)
                                lineTo(bX + barrierW * 0.75f, bY)
                                lineTo(bX + barrierW * 0.75f, bY - clearH)
                                lineTo(bX - barrierW * 0.75f, bY - clearH)
                                lineTo(bX - barrierW * 0.75f, bY)
                                close()
                            }
                            drawPath(gatePath, color.copy(alpha = 0.35f))
                            drawPath(gatePath, color, style = Stroke(width = maxOf(1f, 2.5f * scale)))
                        }
                    }
                }
            }

            // 3. DRAW PLAYER CHARACTER at fixed playerZ depth coordinate
            val playerZ = 12f
            val pX = projectX(gameEngine.playerLanePosition, playerZ)
            val pY_road = projectY(gameEngine.playerLanePosition, playerZ)
            val pScale = projectScale(playerZ)

            // Calculate active height (Jumping elevates graphic upward)
            val pY_current = pY_road - (gameEngine.playerY * 46.dp.toPx() * pScale)

            // Shadow blob under player on the road
            drawOval(
                color = Color.Black.copy(alpha = 0.65f),
                topLeft = Offset(pX - 22f * pScale, pY_road - 5f * pScale),
                size = Size(44f * pScale, 10f * pScale)
            )

            // Player graphics
            val isSliding = gameEngine.slideTicksRemaining > 0
            val bodyHeight = if (isSliding) 28f * pScale else 58f * pScale
            val bodyWidth = if (isSliding) 36f * pScale else 24f * pScale
            val bodyX = pX - bodyWidth / 2f
            val bodyY = pY_current - bodyHeight

            // Draw glowing core silhouette cyborg shadow
            drawRoundRect(
                color = Color(0xFF0F0C24),
                topLeft = Offset(bodyX, bodyY),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(6f, 6f)
            )

            // Border based on speed boost (Warp effects)
            val borderColor = if (gameEngine.speedBoostTicks > 0) Color(0xFFFF007F) else Color(0xFF00F0FF)
            drawRoundRect(
                color = borderColor,
                topLeft = Offset(bodyX, bodyY),
                size = Size(bodyWidth, bodyHeight),
                style = Stroke(width = 3f),
                cornerRadius = CornerRadius(6f, 6f)
            )

            // Cyborg visor/cyber glowing eyes
            val eyeW = bodyWidth * 0.6f
            val eyeH = 4f * pScale
            val eyeX = pX - eyeW / 2
            val eyeY = bodyY + bodyHeight * 0.2f
            drawRect(
                color = if (gameEngine.shieldTicks > 0) Color(0xFF00F0FF) else Color(0xFFFFEA33),
                topLeft = Offset(eyeX, eyeY),
                size = Size(eyeW, eyeH)
            )

            // Warp shadow lines if Speed Boost is active
            if (gameEngine.speedBoostTicks > 0) {
                for (i in 0..4) {
                    val warpLineLength = 55f * pScale
                    val warpX = pX - bodyWidth * 0.6f + (i * bodyWidth * 0.3f)
                    drawLine(
                        color = Color(0xFFFF007F).copy(alpha = 0.45f),
                        start = Offset(warpX, bodyY + bodyHeight),
                        end = Offset(warpX, bodyY + bodyHeight + warpLineLength),
                        strokeWidth = 2f
                    )
                }
            }

            // Shield Bubble Graphic representation
            if (gameEngine.shieldTicks > 0) {
                val shieldRadius = bodyHeight * 0.85f
                drawCircle(
                    color = Color(0xFF00F0FF).copy(alpha = 0.15f * ambientGlowVal),
                    radius = shieldRadius,
                    center = Offset(pX, bodyY + bodyHeight / 2)
                )
                drawCircle(
                    color = Color(0xFF00F0FF).copy(alpha = 0.7f),
                    radius = shieldRadius,
                    center = Offset(pX, bodyY + bodyHeight / 2),
                    style = Stroke(width = 2f)
                )
            }

            // Magnetic Vortex representation
            if (gameEngine.magnetTicks > 0) {
                val circleRadius = bodyHeight * 1.1f * ambientGlowVal
                drawCircle(
                    color = Color(0xFF00FF66).copy(alpha = 0.4f),
                    radius = circleRadius,
                    center = Offset(pX, bodyY + bodyHeight / 2),
                    style = Stroke(width = 1.5f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                )
            }
        }

        // 3. OVERLAY STATUS HUD (Score, coins, powerups trackers)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score Box
                Column(modifier = Modifier.weight(1f)) {
                    Text("DISTANCE", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${distance.toInt()}m", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }

                // Level / Multiplier Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFF007F))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    val activeMultiplier = (1 + (distance / 300f).toInt()) * (if (gameEngine.speedBoostTicks > 0) 2 else 1)
                    Text("x$activeMultiplier", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
                
                Spacer(modifier = Modifier.width(16.dp))

                // Score Display
                Column(horizontalAlignment = Alignment.End) {
                    Text("CORE SCORE", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("$score", color = Color(0xFF00F0FF), fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("current_score_text"))
                }
            }

            // Secondary stats: Coins count & dynamic Powerup indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = "Coins Collected", tint = Color(0xFFFFEA33), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("$coinsCount", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("run_coins_text"))
                }

                // Pause button
                if (activeState == GameState.RUNNING) {
                    IconButton(
                        onClick = { gameEngine.pauseGame() },
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .testTag("pause_game_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Pause", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Power-up indicators
            if (gameEngine.shieldTicks > 0 || gameEngine.speedBoostTicks > 0 || gameEngine.magnetTicks > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (gameEngine.shieldTicks > 0) {
                        BuffIndicator(label = "SHIELD", ratio = gameEngine.shieldTicks.toFloat() / 300f, color = Color(0xFF00F0FF))
                    }
                    if (gameEngine.speedBoostTicks > 0) {
                        BuffIndicator(label = "SPEED Warp", ratio = gameEngine.speedBoostTicks.toFloat() / 300f, color = Color(0xFFFF007F))
                    }
                    if (gameEngine.magnetTicks > 0) {
                        BuffIndicator(label = "MAGNET", ratio = gameEngine.magnetTicks.toFloat() / 300f, color = Color(0xFF00FF66))
                    }
                }
            }
        }

        // Swipe Instructions Overlay when runner starts
        if (activeState == GameState.RUNNING && distance < 80f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = "SWIPE LEFT / RIGHT TO SHIFT LANES\nSWIPE UP TO JUMP | SWIPE DOWN TO SLIDE",
                    color = Color.White.copy(alpha = 0.45f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 4. COUNTDOWN OVERLAY
        if (activeState == GameState.COUNTDOWN) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                val countText = if (gameEngine.countdownValue > 0) "${gameEngine.countdownValue}" else "RUN!"
                Text(
                    text = countText,
                    color = Color(0xFF00F0FF),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 5. PAUSED SCREEN OVERLAY
        if (activeState == GameState.PAUSED) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text("CIRCUIT STOPPED", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Connection to matrix paused temporarily.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, textAlign = TextAlign.Center)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { gameEngine.resumeGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .testTag("resume_game_button")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RESUME", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { gameEngine.exitToMenu(); onNavigateBack() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Text("EXIT RUN", color = Color.White)
                    }
                }
            }
        }

        // 6. GAME OVER OVERLAY
        if (activeState == GameState.GAME_OVER) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131024))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CIRCUIT OVERLOADED",
                            color = Color(0xFFFF0255),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Score Obtained", color = Color.White.copy(alpha = 0.5f))
                            Text("$score", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Distance Traveled", color = Color.White.copy(alpha = 0.5f))
                            Text("${distance.toInt()} meters", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Coins Secured", color = Color.White.copy(alpha = 0.5f))
                            Row {
                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFEA33), modifier = Modifier.size(14.dp))
                                Text("$coinsCount", color = Color(0xFFFFEA33), fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // AdMob Rewarded ad connection (or simulated trigger)
                        Button(
                            onClick = {
                                gameEngine.revivePlayer()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("revive_ad_button")
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("REVIVE WITH AD", color = Color.Black, fontWeight = FontWeight.Black)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { gameEngine.exitToMenu(); onNavigateBack() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("game_over_okay_button")
                        ) {
                            Text("DISCONNECT", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BuffIndicator(label: String, ratio: Float, color: Color) {
    Column(modifier = Modifier.width(64.dp)) {
        Text(
            text = label,
            color = color,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { ratio.coerceIn(0f, 1f) },
            color = color,
            trackColor = Color.White.copy(alpha = 0.15f),
            modifier = Modifier
                .height(3.dp)
                .fillMaxWidth()
                .clip(CircleShape)
        )
    }
}
