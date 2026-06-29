package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val totalCoins: Int = 0,
    val highestScore: Int = 0,
    val totalDistance: Int = 0,
    val selectedCharacterId: String = "sky_kid",
    val unlockedCharacterIds: String = "sky_kid", // comma-separated
    val lastDailyRewardClaimedTime: Long = 0L,
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true
)

@Entity(tableName = "run_history")
data class RunHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val score: Int,
    val coins: Int,
    val distance: Int,
    val dateMillis: Long = System.currentTimeMillis(),
    val playerName: String = "You"
)

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean = false,
    val progress: Int = 0,
    val maxProgress: Int = 1,
    val rewardCoins: Int = 100,
    val unlockTime: Long = 0L
)
