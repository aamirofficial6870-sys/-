package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val username: String = "ShadowRunner",
    val highScore: Long = 0,
    val totalCoins: Int = 0,
    val shieldLevel: Int = 1, // Upgrade levels: 1 to 5
    val speedLevel: Int = 1,
    val magnetLevel: Int = 1,
    val currentLevel: Int = 1,
    val currentXp: Int = 0,
    val lastDailyClaimTime: Long = 0,
    val dailyClaimStreak: Int = 0
)

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val targetProgress: Int,
    val currentProgress: Int,
    val isUnlocked: Boolean = false,
    val rewardCoins: Int = 100
)

@Entity(tableName = "leaderboard")
data class LeaderboardEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playerName: String,
    val score: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
