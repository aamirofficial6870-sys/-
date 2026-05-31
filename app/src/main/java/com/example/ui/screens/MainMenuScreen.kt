package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.game.audio.GameAudio

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    userProfile: UserProfile,
    onNavigateToGame: () -> Unit,
    onNavigateToUpgrades: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToDaily: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onChangeUsername: (String) -> Unit
) {
    var showUsernameDialog by remember { mutableStateOf(false) }
    var tempUsername by remember { mutableStateOf(userProfile.username) }

    // Pulsing background color animation
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07070F))
            .drawBehind {
                // Cyber grid / neon matrix aesthetic background
                val gridDensity = 68.dp.toPx()
                val linePaintWidth = 1.dp.toPx()
                val color = Color(0xFF1F1A3A).copy(alpha = pulseAlpha)
                
                // Draw vertical grid lines
                var x = 0f
                while (x < size.width) {
                    drawLine(color, Offset(x, 0f), Offset(x, size.height), linePaintWidth)
                    x += gridDensity
                }
                
                // Draw horizontal grid lines
                var y = 0f
                while (y < size.height) {
                    drawLine(color, Offset(0f, y), Offset(size.width, y), linePaintWidth)
                    y += gridDensity
                }
            }
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            // Header: Profile state & currency bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Level & XP Ring Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141324))
                        .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { 
                            GameAudio.playClick()
                            showUsernameDialog = true 
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User info",
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = userProfile.username,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "LEVEL ${userProfile.currentLevel}",
                            color = Color(0xFFFFFF33),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Currency display
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141324))
                        .border(1.dp, Color(0xFFFF007F).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Cyber Coins",
                        tint = Color(0xFFFFEA33),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${userProfile.totalCoins}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("currency_text")
                    )
                }
            }

            // Title block
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Text(
                    text = "SHADOW",
                    color = Color.White,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 8.sp,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "ESCAPE",
                    color = Color(0xFF00F0FF),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(y = (-6).dp)
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                
                // High score badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFF007F).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFFF007F), RoundedCornerShape(6.dp))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "HIGH SCORE: ${userProfile.highScore}",
                        color = Color(0xFFFF007F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Core Play Button (Expanded / Pulsing)
            val shadowPulse by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "ShadowPulse"
            )

            Box(
                modifier = Modifier
                    .size(width = 220.dp, height = 74.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF00F0FF), Color(0xFFFF007F))
                        )
                    )
                    .border(
                        width = shadowPulse.dp / 4f,
                        color = Color.White.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        GameAudio.playLevelUp()
                        onNavigateToGame()
                    }
                    .testTag("play_game_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LAUCH RUN",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                }
            }

            // Asymmetric grid menu for optional subsystems
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MenuCard(
                        title = "UPGRADES",
                        subtitle = "Overclock gear",
                        icon = Icons.Default.Build,
                        accentColor = Color(0xFF00F0FF),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("nav_upgrades_button"),
                        onClick = {
                            GameAudio.playClick()
                            onNavigateToUpgrades()
                        }
                    )
                    MenuCard(
                        title = "RANKING",
                        subtitle = "Global circuit",
                        icon = Icons.Default.Menu,
                        accentColor = Color(0xFFFF007F),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("nav_leaderboard_button"),
                        onClick = {
                            GameAudio.playClick()
                            onNavigateToLeaderboard()
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MenuCard(
                        title = "DAILY LOAD",
                        subtitle = "Streak logs",
                        icon = Icons.Default.AddCircle,
                        accentColor = Color(0xFF00FF66),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("nav_daily_button"),
                        onClick = {
                            GameAudio.playClick()
                            onNavigateToDaily()
                        }
                    )
                    MenuCard(
                        title = "MEDALS",
                        subtitle = "Achievements",
                        icon = Icons.Default.ThumbUp,
                        accentColor = Color(0xFFFFEA33),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("nav_achievements_button"),
                        onClick = {
                            GameAudio.playClick()
                            onNavigateToAchievements()
                        }
                    )
                }
            }

            // Small watermark
            Text(
                text = "v1.4 ENDLESS CYBER CIRCUIT TRACE",
                color = Color.White.copy(alpha = 0.25f),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }

    // Username settings dialog
    if (showUsernameDialog) {
        AlertDialog(
            onDismissRequest = { showUsernameDialog = false },
            title = { Text("CYBER REGISTRY", color = Color(0xFF00F0FF), fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text("Identify yourself on the matrix (Max 15 characters):", color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = tempUsername,
                        onValueChange = { if (it.length <= 15) tempUsername = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00F0FF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF)),
                    onClick = {
                        onChangeUsername(tempUsername)
                        showUsernameDialog = false
                    }
                ) {
                    Text("LOCK IN", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUsernameDialog = false }) {
                    Text("CANCEL", color = Color.White.copy(alpha = 0.5f))
                }
            },
            containerColor = Color(0xFF0D0B1C)
        )
    }
}

@Composable
fun MenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF110E24)),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 9.sp
                )
            }
        }
    }
}
