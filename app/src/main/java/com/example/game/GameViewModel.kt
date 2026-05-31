package com.example.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.GameDatabase
import com.example.data.model.Achievement
import com.example.data.model.LeaderboardEntry
import com.example.data.model.UserProfile
import com.example.data.repository.GameRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val db = GameDatabase.getDatabase(application)
    private val repository = GameRepository(db.gameDao())

    // Game Engine instance
    val gameEngine = GameEngine(repository, viewModelScope)

    // Flows for UI observing
    val userProfile: StateFlow<UserProfile> = repository.userProfileFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfile()
        )

    val achievements: StateFlow<List<Achievement>> = repository.achievementsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val leaderboard: StateFlow<List<LeaderboardEntry>> = repository.leaderboardFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Build initial entries
        viewModelScope.launch {
            repository.initializeDatabase()
        }
    }

    // Interactive Triggers
    fun upgrade(powerUpType: String, onCompleted: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.upgradePowerUp(powerUpType)
            onCompleted(result)
        }
    }

    fun claimDailyReward(onResult: (Int?) -> Unit) {
        viewModelScope.launch {
            val amount = repository.claimDailyReward()
            onResult(amount)
        }
    }

    fun changeUsername(newName: String) {
        viewModelScope.launch {
            if (newName.isNotBlank() && newName.length <= 15) {
                repository.setUsername(newName.trim())
            }
        }
    }

    fun forceLeaderboardSync() {
        viewModelScope.launch {
            repository.syncLeaderboardToCloud()
        }
    }

    // Helper cost functions exposed to UI
    fun getUpgradeCostOf(currentLevel: Int): Int {
        return repository.getUpgradeCost(currentLevel)
    }

    fun getXpRequirementOf(currentLevel: Int): Int {
        return repository.getXpNeededForLevel(currentLevel)
    }

    // simulated rewarded ad trigger
    fun triggerRewardedAd(onEarnedReward: () -> Unit) {
        // Real mobile app would load AdMob here, show a progress dialog of mock video.
        // We simulate a clean cyber pop-up countdown that triggers the unlock!
        onEarnedReward()
    }
}
