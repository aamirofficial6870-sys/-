package com.example.data.dao

import androidx.room.*
import com.example.data.model.UserProfile
import com.example.data.model.Achievement
import com.example.data.model.LeaderboardEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    // User Profile Queries
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    @Update
    suspend fun updateUserProfile(profile: UserProfile)

    // Achievements Queries
    @Query("SELECT * FROM achievements")
    fun getAllAchievementsFlow(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements")
    suspend fun getAllAchievements(): List<Achievement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<Achievement>)

    @Update
    suspend fun updateAchievement(achievement: Achievement)

    @Query("SELECT * FROM achievements WHERE id = :id LIMIT 1")
    suspend fun getAchievementById(id: String): Achievement?

    // Leaderboard Queries
    @Query("SELECT * FROM leaderboard ORDER BY score DESC LIMIT 25")
    fun getLeaderboardFlow(): Flow<List<LeaderboardEntry>>

    @Query("SELECT * FROM leaderboard ORDER BY score DESC LIMIT 25")
    suspend fun getLeaderboard(): List<LeaderboardEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaderboardEntry(entry: LeaderboardEntry)

    @Query("DELETE FROM leaderboard")
    suspend fun clearLeaderboard()
}
