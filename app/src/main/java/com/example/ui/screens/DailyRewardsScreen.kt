package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
fun DailyRewardsScreen(
    userProfile: UserProfile,
    onClaimReward: ((Int?) -> Unit) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Calculate claim state
    val oneDayMs = 24 * 60 * 60 * 1000L
    val timeDiff = System.currentTimeMillis() - userProfile.lastDailyClaimTime
    val canClaim = timeDiff >= oneDayMs || userProfile.lastDailyClaimTime == 0L

    // Current daily reward streak day (clamped 1 to 7)
    val nextDayToClaim = if (canClaim) {
        (userProfile.dailyClaimStreak % 7) + 1
    } else {
        userProfile.dailyClaimStreak
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "DAILY CODE LOAD",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { GameAudio.playClick(); onNavigateBack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF07070F))
            )
        },
        containerColor = Color(0xFF07070F)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Streak Header metrics display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF00FF66).copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF110E24))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ACTIVE CLAIM CONSECUTIVE STREAK",
                        color = Color(0xFF00FF66),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${userProfile.dailyClaimStreak} DAYS",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Connect logs consecutive days to overclock your multiplier rewards up to x7!",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calendar daily lockers 7 days grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(7) { index ->
                    val dayNum = index + 1
                    val rewardCoins = dayNum * 100
                    
                    // Logic states for each calendar item
                    // If streak has claimed it, show passed
                    val isPastClaim = dayNum < nextDayToClaim
                    val isClaimableNow = dayNum == nextDayToClaim && canClaim
                    val isLocked = dayNum > nextDayToClaim || (dayNum == nextDayToClaim && !canClaim)

                    val borderColor = when {
                        isClaimableNow -> Color(0xFF00F0FF)
                        isPastClaim -> Color(0xFF00FF66)
                        else -> Color.White.copy(alpha = 0.1f)
                    }

                    val backBgColor = when {
                        isClaimableNow -> Color(0xFF00F0FF).copy(alpha = 0.08f)
                        isPastClaim -> Color(0xFF00FF66).copy(alpha = 0.04f)
                        else -> Color(0xFF110E24)
                    }

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(backBgColor)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Text(
                                text = "DAY 0$dayNum",
                                color = if (isClaimableNow) Color(0xFF00F0FF) else Color.White.copy(alpha = 0.45f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = when {
                                    isPastClaim -> Color(0xFF00FF66)
                                    isClaimableNow -> Color(0xFFFFEA33)
                                    else -> Color.White.copy(alpha = 0.15f)
                                },
                                modifier = Modifier.size(24.dp)
                            )

                            Text(
                                text = "+$rewardCoins",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Claim trigger action button
            if (canClaim) {
                Button(
                    onClick = {
                        onClaimReward { rewardAmount ->
                            if (rewardAmount != null) {
                                GameAudio.playLevelUp()
                                Toast.makeText(context, "CLAIMED $rewardAmount CYBER COINS!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "CLAIM FAILED!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .height(50.dp)
                        .testTag("claim_daily_reward_button")
                ) {
                    Text(
                        text = "LOAD DAILY ENCRYPTION",
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .height(50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NEXT ENCRYPTION IN PROGRESS...",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
