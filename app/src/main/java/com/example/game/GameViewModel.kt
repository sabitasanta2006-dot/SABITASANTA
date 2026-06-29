package com.example.game

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.GameAudioSynthesizer
import com.example.data.AppDatabase
import com.example.data.GameRepository
import com.example.data.RunHistory
import com.example.data.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class Screen {
    SPLASH,
    LOADING,
    MAIN_MENU,
    CHARACTER_SELECT,
    SETTINGS,
    GAMEPLAY,
    GAME_OVER,
    HIGH_SCORES,
    ACHIEVEMENTS,
    DAILY_REWARDS
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository
    val audioSynthesizer = GameAudioSynthesizer()

    // Screen State Navigation
    private val _currentScreen = MutableStateFlow(Screen.SPLASH)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Local Persistence States
    val userProfile: StateFlow<UserProfile> = MutableStateFlow(UserProfile())
    val topScores: StateFlow<List<RunHistory>> = MutableStateFlow(emptyList())
    val achievements: StateFlow<List<com.example.data.Achievement>> = MutableStateFlow(emptyList())

    // Active Gameplay Engine Reference
    private val _activeEngine = MutableStateFlow<GameEngine?>(null)
    val activeEngine: StateFlow<GameEngine?> = _activeEngine.asStateFlow()

    // Last game results for Game Over screen
    private val _lastScore = MutableStateFlow(0)
    val lastScore: StateFlow<Int> = _lastScore.asStateFlow()

    private val _lastCoins = MutableStateFlow(0)
    val lastCoins: StateFlow<Int> = _lastCoins.asStateFlow()

    private val _lastDistance = MutableStateFlow(0)
    val lastDistance: StateFlow<Int> = _lastDistance.asStateFlow()

    // Daily Claim tracking
    private val _dailyClaimStatusMessage = MutableStateFlow<String?>(null)
    val dailyClaimStatusMessage: StateFlow<String?> = _dailyClaimStatusMessage.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao())

        // Connect flows
        viewModelScope.launch {
            repository.userProfile.collect { profile ->
                (userProfile as MutableStateFlow).value = profile
                
                // Sync audio synthesizer state with db settings
                audioSynthesizer.isSoundEnabled = profile.soundEnabled
                audioSynthesizer.isMusicEnabled = profile.musicEnabled
            }
        }

        viewModelScope.launch {
            repository.topScores.collect { scores ->
                (topScores as MutableStateFlow).value = scores
            }
        }

        viewModelScope.launch {
            repository.achievements.collect { list ->
                (achievements as MutableStateFlow).value = list
            }
        }

        // Run splash screen timeline
        runSplashScreenSequence()
    }

    private fun runSplashScreenSequence() {
        viewModelScope.launch {
            delay(1800) // 1.8 seconds of premium splash
            _currentScreen.value = Screen.LOADING
            
            // Artificial assets loading progression
            delay(1200) // 1.2 seconds of loading animation
            _currentScreen.value = Screen.MAIN_MENU
            
            // Start the custom background synth music
            audioSynthesizer.startBackgroundMusic()
        }
    }

    fun navigateTo(screen: Screen) {
        audioSynthesizer.playClick()
        
        // Stop active gameplay if exiting gameplay screen
        if (screen != Screen.GAMEPLAY && _currentScreen.value == Screen.GAMEPLAY) {
            _activeEngine.value?.stop()
            _activeEngine.value = null
        }
        
        _currentScreen.value = screen
    }

    fun startGame() {
        audioSynthesizer.playClick()
        
        val selectedCharId = userProfile.value.selectedCharacterId
        val character = CharacterConfig.ALL_CHARACTERS.find { it.id == selectedCharId } 
            ?: CharacterConfig.ALL_CHARACTERS.first()

        // Instantiate new Game Engine
        val engine = GameEngine(
            characterConfig = character,
            audioSynthesizer = audioSynthesizer,
            onGameOver = { score, coins, distance ->
                viewModelScope.launch {
                    _lastScore.value = score
                    _lastCoins.value = coins
                    _lastDistance.value = distance
                    
                    // Save results to Database
                    repository.saveGameResult(score, coins, distance)
                    
                    // Route to GAME_OVER screen
                    _currentScreen.value = Screen.GAME_OVER
                }
            }
        )

        _activeEngine.value = engine
        _currentScreen.value = Screen.GAMEPLAY
        engine.start()
    }

    fun pauseGame() {
        audioSynthesizer.playClick()
        _activeEngine.value?.pause()
    }

    fun triggerJump() {
        _activeEngine.value?.jump()
    }

    fun triggerSlide() {
        _activeEngine.value?.slide()
    }

    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(sound = enabled, music = userProfile.value.musicEnabled)
        }
    }

    fun toggleMusic(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(sound = userProfile.value.soundEnabled, music = enabled)
        }
    }

    fun selectCharacter(charId: String) {
        viewModelScope.launch {
            repository.selectCharacter(charId)
            audioSynthesizer.playClick()
        }
    }

    fun unlockCharacter(charId: String, cost: Int) {
        viewModelScope.launch {
            val success = repository.buyCharacter(charId, cost)
            if (success) {
                audioSynthesizer.playPowerUp() // positive sound
            } else {
                audioSynthesizer.playDamage() // negative error sound
            }
        }
    }

    fun claimDailyReward() {
        viewModelScope.launch {
            val success = repository.claimDailyReward(250) // Grants 250 free coins!
            if (success) {
                audioSynthesizer.playCoin()
                _dailyClaimStatusMessage.value = "SUCCESS: Claimed 250 gold coins!"
                // Grant double jump achievement first-jump if clicked first
                repository.updateAchievementProgress("first_jump", 1)
            } else {
                audioSynthesizer.playDamage()
                _dailyClaimStatusMessage.value = "ERROR: Reward already claimed today. Try again later!"
            }
            delay(3000)
            _dailyClaimStatusMessage.value = null
        }
    }

    fun resetData() {
        viewModelScope.launch {
            repository.resetGameData()
            audioSynthesizer.playDamage()
        }
    }

    override fun onCleared() {
        super.onCleared()
        _activeEngine.value?.stop()
        audioSynthesizer.stopBackgroundMusic()
    }
}
