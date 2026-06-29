package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    @Update
    suspend fun updateUserProfile(profile: UserProfile)

    @Query("SELECT * FROM run_history ORDER BY score DESC LIMIT :limit")
    fun getTopScoresFlow(limit: Int): Flow<List<RunHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRunHistory(run: RunHistory)

    @Query("SELECT * FROM achievements")
    fun getAllAchievementsFlow(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements")
    suspend fun getAllAchievements(): List<Achievement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<Achievement>)

    @Update
    suspend fun updateAchievement(achievement: Achievement)
    
    @Query("DELETE FROM run_history")
    suspend fun clearAllHistory()
}
