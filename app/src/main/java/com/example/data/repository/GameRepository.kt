package com.example.data.repository

import com.example.data.dao.GameDao
import com.example.data.model.UserProfile
import com.example.data.model.Achievement
import com.example.data.model.LeaderboardEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class GameRepository(private val gameDao: GameDao) {

    // Seed checking & flows
    val userProfileFlow: Flow<UserProfile> = gameDao.getUserProfileFlow()
        .map { it ?: UserProfile() }
        .flowOn(Dispatchers.IO)

    val achievementsFlow: Flow<List<Achievement>> = gameDao.getAllAchievementsFlow()
        .flowOn(Dispatchers.IO)

    val leaderboardFlow: Flow<List<LeaderboardEntry>> = gameDao.getLeaderboardFlow()
        .flowOn(Dispatchers.IO)

    // Ensure database contains essential initial values
    suspend fun initializeDatabase() = withContext(Dispatchers.IO) {
        val currentProfile = gameDao.getUserProfile()
        if (currentProfile == null) {
            gameDao.insertUserProfile(UserProfile())
        }

        val currentAchievements = gameDao.getAllAchievements()
        if (currentAchievements.isEmpty()) {
            val defaults = listOf(
                Achievement("first_run", "Shadow Spark", "Complete 1 complete run", 1, 0, false, 150),
                Achievement("collect_coins", "Silicon Glutton", "Collect 250 cyber coins in total", 250, 0, false, 300),
                Achievement("score_milestone", "Grid Breaker", "Reach a score of 5,000 points in one run", 5000, 0, false, 500),
                Achievement("use_powerups", "System Hack", "Collect 15 active power-ups", 15, 0, false, 250),
                Achievement("upgrade_max", "Overclocked", "Upgrade any power-up to level 5", 5, 1, false, 1000)
            )
            gameDao.insertAchievements(defaults)
        }

        // Initialize seed leaderboard entries if empty
        val leaderboard = gameDao.getLeaderboard()
        if (leaderboard.isEmpty()) {
            val seedLeaderboard = listOf(
                LeaderboardEntry(playerName = "NeonReaper", score = 15400, timestamp = System.currentTimeMillis() - 86400000),
                LeaderboardEntry(playerName = "Zero_Cool", score = 11200, timestamp = System.currentTimeMillis() - 172800000),
                LeaderboardEntry(playerName = "Shadow_Walker", score = 8450, timestamp = System.currentTimeMillis() - 259200000),
                LeaderboardEntry(playerName = "GlitchCatcher", score = 5900, timestamp = System.currentTimeMillis() - 345600000)
            )
            seedLeaderboard.forEach { gameDao.insertLeaderboardEntry(it) }
        }
    }

    // Submit Game Run Scores & Update Profile/Progression
    suspend fun submitGameRun(score: Long, coinsCollected: Int) = withContext(Dispatchers.IO) {
        val profile = gameDao.getUserProfile() ?: UserProfile()
        
        // Calculate new XP & potential level progression
        val xpEarnt = (score / 10).toInt() + (coinsCollected * 2)
        var newXp = profile.currentXp + xpEarnt
        var newLevel = profile.currentLevel
        var xpRequired = getXpNeededForLevel(newLevel)
        while (newXp >= xpRequired) {
            newXp -= xpRequired
            newLevel++
            xpRequired = getXpNeededForLevel(newLevel)
        }

        val isNewHighScore = score > profile.highScore
        val newHighScore = if (isNewHighScore) score else profile.highScore
        val updatedProfile = profile.copy(
            highScore = newHighScore,
            totalCoins = profile.totalCoins + coinsCollected,
            currentLevel = newLevel,
            currentXp = newXp
        )
        gameDao.insertUserProfile(updatedProfile)

        // Record on local leaderboard
        if (score > 100) {
            gameDao.insertLeaderboardEntry(
                LeaderboardEntry(playerName = profile.username, score = score)
            )
        }

        // Update achievements progress
        incrementAchievementProgress("first_run", 1)
        incrementAchievementProgress("collect_coins", coinsCollected)
        if (score >= 5000) {
            updateAchievementValue("score_milestone", score.toInt())
        }
    }

    // Upgrades System
    suspend fun upgradePowerUp(powerUpType: String): Boolean = withContext(Dispatchers.IO) {
        val profile = gameDao.getUserProfile() ?: UserProfile()
        val currentLevel = when (powerUpType) {
            "shield" -> profile.shieldLevel
            "speed" -> profile.speedLevel
            "magnet" -> profile.magnetLevel
            else -> 1
        }

        if (currentLevel >= 5) return@withContext false // Fully upgraded

        val cost = getUpgradeCost(currentLevel)
        if (profile.totalCoins < cost) return@withContext false

        val updatedProfile = when (powerUpType) {
            "shield" -> profile.copy(totalCoins = profile.totalCoins - cost, shieldLevel = currentLevel + 1)
            "speed" -> profile.copy(totalCoins = profile.totalCoins - cost, speedLevel = currentLevel + 1)
            "magnet" -> profile.copy(totalCoins = profile.totalCoins - cost, magnetLevel = currentLevel + 1)
            else -> profile
        }

        gameDao.insertUserProfile(updatedProfile)

        // Check upgrade_max achievement
        val maxLevelReached = maxOf(updatedProfile.shieldLevel, updatedProfile.speedLevel, updatedProfile.magnetLevel)
        updateAchievementValue("upgrade_max", maxLevelReached)
        
        return@withContext true
    }

    // Claim Daily Reward
    suspend fun claimDailyReward(): Int? = withContext(Dispatchers.IO) {
        val profile = gameDao.getUserProfile() ?: UserProfile()
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - profile.lastDailyClaimTime

        val oneDayMs = 24 * 60 * 60 * 1000L
        val twoDaysMs = 48 * 60 * 60 * 1000L

        // Double checking time requirements (e.g. 24 hour cooldown)
        if (timeDiff < oneDayMs && profile.lastDailyClaimTime > 0) {
            // Already claimed within 24 hours
            return@withContext null
        }

        // Determine reward based on streak
        val isStreakActive = timeDiff in oneDayMs..twoDaysMs || profile.lastDailyClaimTime == 0L
        val currentStreak = if (isStreakActive) (profile.dailyClaimStreak % 7) + 1 else 1
        val rewardAmount = currentStreak * 100

        val updatedProfile = profile.copy(
            totalCoins = profile.totalCoins + rewardAmount,
            lastDailyClaimTime = currentTime,
            dailyClaimStreak = currentStreak
        )
        gameDao.insertUserProfile(updatedProfile)
        return@withContext rewardAmount
    }

    // Update username inside profile
    suspend fun setUsername(name: String) = withContext(Dispatchers.IO) {
        val profile = gameDao.getUserProfile() ?: UserProfile()
        gameDao.insertUserProfile(profile.copy(username = name))
    }

    // Firebase simulation leaderboard synchronization
    suspend fun syncLeaderboardToCloud() = withContext(Dispatchers.IO) {
        // Here, of course, in a real implementation we would call Firestore API.
        // We simulate a network API call, tagging our local matches as synced.
        val unSynced = gameDao.getLeaderboard().filter { !it.isSynced }
        unSynced.forEach {
            gameDao.insertLeaderboardEntry(it.copy(isSynced = true))
        }
    }

    // Helper functions for levels, upgrades, xp formulas
    fun getXpNeededForLevel(level: Int): Int {
        return level * 500
    }

    fun getUpgradeCost(currentLevel: Int): Int {
        return currentLevel * 400
    }

    private suspend fun incrementAchievementProgress(id: String, increment: Int) {
        val achievement = gameDao.getAchievementById(id) ?: return
        if (achievement.isUnlocked) return

        val newProgress = minOf(achievement.currentProgress + increment, achievement.targetProgress)
        val isUnlocked = newProgress >= achievement.targetProgress
        val updated = achievement.copy(
            currentProgress = newProgress,
            isUnlocked = isUnlocked
        )
        gameDao.updateAchievement(updated)

        if (isUnlocked) {
            // Reward coins immediately
            val profile = gameDao.getUserProfile() ?: return
            gameDao.insertUserProfile(profile.copy(totalCoins = profile.totalCoins + achievement.rewardCoins))
        }
    }

    private suspend fun updateAchievementValue(id: String, value: Int) {
        val achievement = gameDao.getAchievementById(id) ?: return
        if (achievement.isUnlocked) return

        val newProgress = minOf(maxOf(value, achievement.currentProgress), achievement.targetProgress)
        val isUnlocked = newProgress >= achievement.targetProgress
        val updated = achievement.copy(
            currentProgress = newProgress,
            isUnlocked = isUnlocked
        )
        gameDao.updateAchievement(updated)

        if (isUnlocked) {
            val profile = gameDao.getUserProfile() ?: return
            gameDao.insertUserProfile(profile.copy(totalCoins = profile.totalCoins + achievement.rewardCoins))
        }
    }

    suspend fun incrementPowerUpsPower() {
        incrementAchievementProgress("use_powerups", 1)
    }
}
