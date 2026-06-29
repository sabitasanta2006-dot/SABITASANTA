package com.example.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class GameRepository(private val gameDao: GameDao) {

    val userProfile: Flow<UserProfile> = gameDao.getUserProfileFlow()
        .map { profile ->
            profile ?: UserProfile().also { defaultProfile ->
                // Ensure there is always a default profile in the DB
                try {
                    gameDao.insertUserProfile(defaultProfile)
                } catch (e: Exception) {
                    Log.e("GameRepository", "Failed to insert default user profile", e)
                }
            }
        }

    val topScores: Flow<List<RunHistory>> = gameDao.getTopScoresFlow(10)
        .onStart {
            // Seed a few fun mock scores to make the leaderboard look active on first load
            val existing = gameDao.getTopScoresFlow(10).firstOrNull() ?: emptyList()
            if (existing.isEmpty()) {
                val seedData = listOf(
                    RunHistory(score = 8500, coins = 120, distance = 1800, playerName = "AeroJet"),
                    RunHistory(score = 6200, coins = 95, distance = 1400, playerName = "CloudKicker"),
                    RunHistory(score = 4100, coins = 60, distance = 950, playerName = "WindRider"),
                    RunHistory(score = 2500, coins = 30, distance = 600, playerName = "StarDuster")
                )
                for (history in seedData) {
                    gameDao.insertRunHistory(history)
                }
            }
        }

    val achievements: Flow<List<Achievement>> = gameDao.getAllAchievementsFlow()
        .onStart {
            val existing = gameDao.getAllAchievements()
            if (existing.isEmpty()) {
                val defaults = listOf(
                    Achievement("first_jump", "First Leap", "Perform your first double jump!", maxProgress = 1, rewardCoins = 100),
                    Achievement("coin_100", "Gold Digger", "Accumulate 100 gold coins in total", maxProgress = 100, rewardCoins = 200),
                    Achievement("dist_1000", "Sky Cruiser", "Run 1000 meters in a single run", maxProgress = 1000, rewardCoins = 300),
                    Achievement("score_5000", "Elite Runner", "Achieve a high score of 5000 points", maxProgress = 5000, rewardCoins = 500),
                    Achievement("unlock_char", "Aero Squad", "Unlock any premium character", maxProgress = 1, rewardCoins = 250)
                )
                gameDao.insertAchievements(defaults)
            }
        }

    suspend fun saveGameResult(score: Int, coins: Int, distance: Int) {
        val currentProfile = gameDao.getUserProfile() ?: UserProfile()
        val newCoins = currentProfile.totalCoins + coins
        val newHighScore = if (score > currentProfile.highestScore) score else currentProfile.highestScore
        val newTotalDistance = currentProfile.totalDistance + distance

        val updatedProfile = currentProfile.copy(
            totalCoins = newCoins,
            highestScore = newHighScore,
            totalDistance = newTotalDistance
        )
        gameDao.insertUserProfile(updatedProfile)

        // Insert history item
        gameDao.insertRunHistory(RunHistory(score = score, coins = coins, distance = distance))

        // Update progress-based achievements
        updateAchievementProgress("coin_100", coins)
        updateAchievementProgress("dist_1000", distance)
        updateAchievementProgress("score_5000", score)
    }

    suspend fun updateAchievementProgress(id: String, increment: Int) {
        val all = gameDao.getAllAchievements()
        val match = all.find { it.id == id } ?: return
        if (match.isUnlocked) return

        val newProgress = if (id == "dist_1000" || id == "score_5000") {
            // These require reaching a threshold in a single run, so we set to max of current progress and target
            maxOf(match.progress, increment)
        } else {
            // Cumulative increment
            match.progress + increment
        }

        val unlocked = newProgress >= match.maxProgress
        val updated = match.copy(
            progress = minOf(newProgress, match.maxProgress),
            isUnlocked = unlocked,
            unlockTime = if (unlocked && !match.isUnlocked) System.currentTimeMillis() else match.unlockTime
        )
        gameDao.updateAchievement(updated)

        // If newly unlocked, grant reward coins
        if (unlocked && !match.isUnlocked) {
            val profile = gameDao.getUserProfile() ?: UserProfile()
            gameDao.insertUserProfile(profile.copy(totalCoins = profile.totalCoins + match.rewardCoins))
        }
    }

    suspend fun triggerAchievementUnlock(id: String) {
        updateAchievementProgress(id, 999999) // Force unlock
    }

    suspend fun updateSettings(sound: Boolean, music: Boolean) {
        val profile = gameDao.getUserProfile() ?: UserProfile()
        gameDao.updateUserProfile(profile.copy(soundEnabled = sound, musicEnabled = music))
    }

    suspend fun selectCharacter(charId: String) {
        val profile = gameDao.getUserProfile() ?: UserProfile()
        if (profile.unlockedCharacterIds.split(",").contains(charId)) {
            gameDao.updateUserProfile(profile.copy(selectedCharacterId = charId))
        }
    }

    suspend fun buyCharacter(charId: String, cost: Int): Boolean {
        val profile = gameDao.getUserProfile() ?: UserProfile()
        val unlockedList = profile.unlockedCharacterIds.split(",").toMutableList()
        
        if (unlockedList.contains(charId)) return true
        if (profile.totalCoins >= cost) {
            unlockedList.add(charId)
            val updated = profile.copy(
                totalCoins = profile.totalCoins - cost,
                unlockedCharacterIds = unlockedList.joinToString(","),
                selectedCharacterId = charId
            )
            gameDao.updateUserProfile(updated)
            triggerAchievementUnlock("unlock_char")
            return true
        }
        return false
    }

    suspend fun claimDailyReward(coins: Int): Boolean {
        val profile = gameDao.getUserProfile() ?: UserProfile()
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000
        
        if (now - profile.lastDailyRewardClaimedTime >= oneDayMillis) {
            gameDao.updateUserProfile(
                profile.copy(
                    totalCoins = profile.totalCoins + coins,
                    lastDailyRewardClaimedTime = now
                )
            )
            return true
        }
        return false
    }

    suspend fun resetGameData() {
        gameDao.clearAllHistory()
        val defaults = listOf(
            Achievement("first_jump", "First Leap", "Perform your first double jump!", maxProgress = 1, rewardCoins = 100),
            Achievement("coin_100", "Gold Digger", "Accumulate 100 gold coins in total", maxProgress = 100, rewardCoins = 200),
            Achievement("dist_1000", "Sky Cruiser", "Run 1000 meters in a single run", maxProgress = 1000, rewardCoins = 300),
            Achievement("score_5000", "Elite Runner", "Achieve a high score of 5000 points", maxProgress = 5000, rewardCoins = 500),
            Achievement("unlock_char", "Aero Squad", "Unlock any premium character", maxProgress = 1, rewardCoins = 250)
        )
        gameDao.insertAchievements(defaults)
        gameDao.insertUserProfile(UserProfile())
    }
}
