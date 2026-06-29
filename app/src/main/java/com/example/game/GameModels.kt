package com.example.game

import androidx.compose.ui.graphics.Color

enum class CharacterState {
    RUNNING, JUMPING, DOUBLE_JUMPING, SLIDING, DEAD
}

enum class ObstacleType {
    SPIKES,
    FLYING_BIRD,
    ROBOT,
    GHOST,
    ROLLING_STONE,
    FIRE_TRAP,
    FALLING_PLATFORM
}

enum class PowerUpType(val displayName: String, val color: Color) {
    COIN_MAGNET("Magnet", Color(0xFFE91E63)),
    SPEED_BOOST("Speed Boost", Color(0xFFFF9800)),
    SHIELD("Shield Bubble", Color(0xFF03A9F4)),
    DOUBLE_COINS("2x Coins", Color(0xFFFFEB3B)),
    EXTRA_LIFE("Extra Life", Color(0xFF4CAF50))
}

data class CharacterConfig(
    val id: String,
    val name: String,
    val description: String,
    val cost: Int,
    val primaryColor: Color,
    val accentColor: Color,
    val abilityText: String
) {
    companion object {
        val ALL_CHARACTERS = listOf(
            CharacterConfig(
                id = "sky_kid",
                name = "Sky Kid",
                description = "Energetic default runner. Always ready to fly!",
                cost = 0,
                primaryColor = Color(0xFF2196F3), // Blue
                accentColor = Color(0xFFFFC107), // Amber
                abilityText = "None (Standard)"
            ),
            CharacterConfig(
                id = "aero_girl",
                name = "Aero Girl",
                description = "Lightweight aerial specialist.",
                cost = 500,
                primaryColor = Color(0xFFE91E63), // Pink
                accentColor = Color(0xFF9C27B0), // Purple
                abilityText = "Higher jumps and softer landings"
            ),
            CharacterConfig(
                id = "cyber_ninja",
                name = "Cyber Ninja",
                description = "High-tech runner with neon trails.",
                cost = 1200,
                primaryColor = Color(0xFF00E676), // Neon Green
                accentColor = Color(0xFF00E5FF), // Cyan
                abilityText = "Slide lasts longer with invulnerability frames"
            ),
            CharacterConfig(
                id = "star_knight",
                name = "Star Knight",
                description = "Golden knight of the celestial heavens.",
                cost = 2500,
                primaryColor = Color(0xFFFFD700), // Gold
                accentColor = Color(0xFFFF5722), // Deep Orange
                abilityText = "Magnet power-ups last twice as long"
            )
        )
    }
}

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float,
    var color: Color,
    var life: Float,      // 0.0 to 1.0 (1.0 = spawn, 0.0 = dead)
    val decay: Float,     // how fast it dies
    val gravity: Float = 0.1f
)

data class Coin(
    val id: String,
    var x: Float,
    var y: Float,
    var radius: Float = 16f,
    var isCollected: Boolean = false,
    var isDouble: Boolean = false
)

data class PowerUpItem(
    val id: String,
    var x: Float,
    var y: Float,
    val type: PowerUpType,
    var isCollected: Boolean = false,
    var radius: Float = 20f
)

data class Obstacle(
    val id: String,
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    val type: ObstacleType,
    var isPassed: Boolean = false,
    var speedX: Float = 0f, // For moving obstacles like birds/robots
    var stateTime: Float = 0f, // For animation
    var verticalMovement: Boolean = false, // Floating platform motion
    var startY: Float = y
)
