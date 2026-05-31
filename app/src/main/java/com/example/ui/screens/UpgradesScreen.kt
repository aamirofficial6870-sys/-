package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.game.audio.GameAudio

data class UpgradeItem(
    val type: String,
    val name: String,
    val description: String,
    val currentLevel: Int,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradesScreen(
    userProfile: UserProfile,
    onUpgrade: (String, (Boolean) -> Unit) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val upgradesList = listOf(
        UpgradeItem(
            type = "shield",
            name = "AEGIS SHIELD",
            description = "Absorbs a single crash collision safely. Increases protective buffer time.",
            currentLevel = userProfile.shieldLevel,
            icon = Icons.Default.Face,
            color = Color(0xFF00F0FF)
        ),
        UpgradeItem(
            type = "speed",
            name = "WARP TUNNEL",
            description = "Boosts forward velocity speed immediately. Grants a score point multiplier.",
            currentLevel = userProfile.speedLevel,
            icon = Icons.Default.ArrowBack, // simple visual icon
            color = Color(0xFFFF007F)
        ),
        UpgradeItem(
            type = "magnet",
            name = "VORTEX MAGNET",
            description = "Pulls in nearby cyber coin structures from adjoining lanes dynamically.",
            currentLevel = userProfile.magnetLevel,
            icon = Icons.Default.Build,
            color = Color(0xFF00FF66)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "EQUIPMENT WORKBENCH",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF07070F)),
                actions = {
                    // Balance status
                    Row(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF141324))
                            .border(1.dp, Color(0xFFFFEA33).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFEA33), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${userProfile.totalCoins}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            )
        },
        containerColor = Color(0xFF07070F)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF110E24))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "OVERCLOCK GEAR POWER",
                            color = Color(0xFF00F0FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Spend currency collected on the circuit run grid to prolong power-up durations of interest.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            items(upgradesList) { upgrade ->
                val cost = upgrade.currentLevel * 400
                val isMaxLevel = upgrade.currentLevel >= 5

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, upgrade.color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF110E24))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(upgrade.color.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = upgrade.icon,
                                        contentDescription = null,
                                        tint = upgrade.color,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = upgrade.name,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isMaxLevel) "MAX REINFORCEMENT" else "UPGRADE PATH",
                                        color = if (isMaxLevel) Color(0xFF00FF66) else Color.White.copy(alpha = 0.4f),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Dynamic Visual tick level indicator
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (i in 1..5) {
                                    val active = i <= upgrade.currentLevel
                                    Box(
                                        modifier = Modifier
                                            .size(width = 12.dp, height = 6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (active) upgrade.color else Color.White.copy(alpha = 0.1f)
                                            )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = upgrade.description,
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (!isMaxLevel) {
                            Button(
                                onClick = {
                                    onUpgrade(upgrade.type) { success ->
                                        if (success) {
                                            GameAudio.playLevelUp()
                                            Toast.makeText(context, "${upgrade.name} UPGRADED!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            GameAudio.playCrash()
                                            Toast.makeText(context, "INSUFFICIENT FUNDS!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (userProfile.totalCoins >= cost) upgrade.color else Color.White.copy(alpha = 0.08f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("upgrade_${upgrade.type}_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (userProfile.totalCoins >= cost) Color.Black else Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "OVERCLOCK FOR $cost COINS",
                                        color = if (userProfile.totalCoins >= cost) Color.Black else Color.White.copy(alpha = 0.3f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(upgrade.color.copy(alpha = 0.07f))
                                    .border(1.dp, upgrade.color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "MAX LEVEL ACHIEVED",
                                    color = upgrade.color,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
