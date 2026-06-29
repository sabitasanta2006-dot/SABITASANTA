package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SkyRunnerApp(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF0D0D26) // Sky Runner cosmic dark background
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                Screen.SPLASH -> SplashScreen()
                Screen.LOADING -> LoadingScreen()
                Screen.MAIN_MENU -> MainMenuScreen(viewModel, profile)
                Screen.CHARACTER_SELECT -> CharacterSelectScreen(viewModel, profile)
                Screen.SETTINGS -> SettingsScreen(viewModel, profile)
                Screen.GAMEPLAY -> GameplayScreen(viewModel)
                Screen.GAME_OVER -> GameOverScreen(viewModel)
                Screen.HIGH_SCORES -> HighScoresScreen(viewModel)
                Screen.ACHIEVEMENTS -> AchievementsScreen(viewModel)
                Screen.DAILY_REWARDS -> DailyRewardsScreen(viewModel, profile)
            }
        }
    }
}

// ==========================================
// 1. SPLASH SCREEN
// ==========================================
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CloudQueue,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier
                    .size(120.dp)
                    .shadow(10.dp, CircleShape)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "SKY RUNNER",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.shadow(8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ENDLESS RETRO FLIGHT",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFB300),
                letterSpacing = 3.sp
            )
        }
    }
}

// ==========================================
// 2. LOADING SCREEN
// ==========================================
@Composable
fun LoadingScreen() {
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (progress < 1.0f) {
            delay(30)
            progress += 0.025f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "GENERATING LEVEL...",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))
            LinearProgressIndicator(
                progress = { progress },
                color = Color(0xFF00E5FF),
                trackColor = Color(0xFF334155),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            // Random cute tip
            val tip = remember {
                listOf(
                    "TIP: Double tap to jump twice as high over tall barriers!",
                    "TIP: Collect the Magnet powerup to attract gold coins effortlessly!",
                    "TIP: The Shield bubble protects you from a single hazard impact!",
                    "TIP: Cyber Ninja gains extra invulnerability frames while sliding!",
                    "TIP: Claim your Daily Rewards to unlock premium high-flying characters!"
                ).random()
            }
            Text(
                text = tip,
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==========================================
// 3. MAIN MENU
// ==========================================
@Composable
fun MainMenuScreen(viewModel: GameViewModel, profile: UserProfile) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1E1E38), Color(0xFF0F0F26))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Profile Stat Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Coins Pill
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Coins",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${profile.totalCoins}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                // High Score Pill
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "High Score",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BEST: ${profile.highestScore}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            // Game Logo Box
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Text(
                    text = "SKY RUNNER",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Endless Flight Horizon",
                    fontSize = 16.sp,
                    color = Color(0xFFFFC107),
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Interactive Play Button
            Button(
                onClick = { viewModel.startGame() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(64.dp)
                    .testTag("play_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "START RUN",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Quick Menu Icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MenuIconBtn(
                    icon = Icons.Default.People,
                    label = "Heroes",
                    onClick = { viewModel.navigateTo(Screen.CHARACTER_SELECT) }
                )
                MenuIconBtn(
                    icon = Icons.Default.EmojiEvents,
                    label = "Scores",
                    onClick = { viewModel.navigateTo(Screen.HIGH_SCORES) }
                )
                MenuIconBtn(
                    icon = Icons.Default.Star,
                    label = "Badges",
                    onClick = { viewModel.navigateTo(Screen.ACHIEVEMENTS) }
                )
                MenuIconBtn(
                    icon = Icons.Default.CardGiftcard,
                    label = "Daily",
                    onClick = { viewModel.navigateTo(Screen.DAILY_REWARDS) }
                )
                MenuIconBtn(
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    onClick = { viewModel.navigateTo(Screen.SETTINGS) }
                )
            }

            // Footer / Ad-Removal Promo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DoNotDisturbOn,
                    contentDescription = "Remove Ads",
                    tint = Color(0xFFFF5722),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Remove Ads & Get 1000 Coins: $0.99 (Simulated Shop)",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun MenuIconBtn(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.LightGray,
            fontWeight = FontWeight.Bold
        )
    }
}

// ==========================================
// 4. CHARACTER SELECTION
// ==========================================
@Composable
fun CharacterSelectScreen(viewModel: GameViewModel, profile: UserProfile) {
    val unlockedIds = remember(profile.unlockedCharacterIds) {
        profile.unlockedCharacterIds.split(",")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.MAIN_MENU) }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "SELECT HERO",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = "Coins", tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${profile.totalCoins}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Characters List (Scrollable)
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(CharacterConfig.ALL_CHARACTERS) { char ->
                    val isUnlocked = unlockedIds.contains(char.id)
                    val isSelected = profile.selectedCharacterId == char.id

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) char.primaryColor else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mini Character Avatar Placeholder with their colors
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(char.primaryColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(char.accentColor, CircleShape)
                                )
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = char.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = char.description,
                                    fontSize = 12.sp,
                                    color = Color.LightGray,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = "Ability",
                                        tint = Color(0xFFFFC107),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = char.abilityText,
                                        fontSize = 11.sp,
                                        color = Color(0xFFFFC107),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Select/Unlock CTA
                            if (isUnlocked) {
                                if (isSelected) {
                                    Button(
                                        onClick = {},
                                        enabled = false,
                                        colors = ButtonDefaults.buttonColors(
                                            disabledContainerColor = Color.White.copy(alpha = 0.1f),
                                            disabledContentColor = char.primaryColor
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("ACTIVE", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.selectCharacter(char.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = char.primaryColor),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("SELECT")
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.unlockCharacter(char.id, char.cost) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = profile.totalCoins >= char.cost
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("${char.cost}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. SETTINGS SCREEN
// ==========================================
@Composable
fun SettingsScreen(viewModel: GameViewModel, profile: UserProfile) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.navigateTo(Screen.MAIN_MENU) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SETTINGS",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Sound Setting Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (profile.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                    contentDescription = "Sound SFX",
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Sound Effects", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("Dynamic synthetic retro audio sfx", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                            Switch(
                                checked = profile.soundEnabled,
                                onCheckedChange = { viewModel.toggleSound(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (profile.musicEnabled) Icons.Default.MusicNote else Icons.Default.MusicOff,
                                    contentDescription = "Music",
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Background Music", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("Infinite 8-bit pentatonic melody loop", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                            Switch(
                                checked = profile.musicEnabled,
                                onCheckedChange = { viewModel.toggleMusic(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Danger Zone Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1E1E)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reset Game Data", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Permanently wipes your high scores, coins, and unlocks.", color = Color.LightGray, fontSize = 11.sp)
                        }
                        Button(
                            onClick = { viewModel.resetData() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("RESET")
                        }
                    }
                }
            }

            // About block
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SKY RUNNER v1.0.0", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Powered by Jetpack Compose Canvas Engine", color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

// ==========================================
// 6. GAMEPLAY
// ==========================================
@Composable
fun GameplayScreen(viewModel: GameViewModel) {
    val gameEngine by viewModel.activeEngine.collectAsState() ?: return
    val score by gameEngine.score.collectAsState()
    val coins by gameEngine.coinsCollected.collectAsState()
    val distance by gameEngine.distance.collectAsState()
    val health by gameEngine.health.collectAsState()
    val isPaused by gameEngine.isPaused.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Core game canvas drawing!
        GameCanvas(
            game = gameEngine,
            modifier = Modifier.fillMaxSize(),
            onTap = { gameEngine.jump() }
        )

        // HUD Elements (Top Row)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.TopCenter
        ) {
            // Stats Group
            Column {
                // Lives Heart Icons
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Life",
                            tint = if (i < health) Color.Red else Color.DarkGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Score + Distance
                Column(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(text = "SCORE: $score", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = "DIST: ${distance}m", color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Right Coins & Active Power-Ups
            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Coins
                    Row(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = "Coins", tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "$coins", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Pause Button
                    IconButton(
                        onClick = { viewModel.pauseGame() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Active Power-Up Badges Ticker
                gameEngine.activePowerUps.forEach { (type, framesLeft) ->
                    val sec = (framesLeft / 60) + 1
                    Box(
                        modifier = Modifier
                            .padding(vertical = 3.dp)
                            .background(type.color.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${type.displayName.uppercase()}: ${sec}s",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // On-Screen Tactile Game Controls (Bottom Row)
        // Placing big JUMP & SLIDE pads on the corners of the screens
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.BottomCenter
        ) {
            // Slide controller (Left hand)
            Button(
                onClick = { gameEngine.slide() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .size(110.dp, 75.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                contentPadding = PaddingValues(0.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "Slide", tint = Color.White, modifier = Modifier.size(24.dp))
                    Text("SLIDE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Jump controller (Right hand)
            Button(
                onClick = { gameEngine.jump() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .size(110.dp, 75.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                contentPadding = PaddingValues(0.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Jump", tint = Color.White, modifier = Modifier.size(24.dp))
                    Text("JUMP", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Pause overlay modal
        if (isPaused) {
            Dialog(onDismissRequest = { viewModel.pauseGame() }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("GAME PAUSED", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { viewModel.pauseGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("RESUME", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.startGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("RESTART RUN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.navigateTo(Screen.MAIN_MENU) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("QUIT TO MENU", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. GAME OVER SCREEN WITH ADS INTEGRATION
// ==========================================
@Composable
fun GameOverScreen(viewModel: GameViewModel) {
    val score by viewModel.lastScore.collectAsState()
    val coins by viewModel.lastCoins.collectAsState()
    val dist by viewModel.lastDistance.collectAsState()

    var adWatching by remember { mutableStateOf(false) }
    var adTimer by remember { mutableStateOf(3) }
    var doubleCoinsSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(adWatching) {
        if (adWatching) {
            while (adTimer > 0) {
                delay(1000)
                adTimer--
            }
            // Ad completed! Reward user
            adWatching = false
            doubleCoinsSuccess = true
            // Play success sfx
            viewModel.audioSynthesizer.playPowerUp()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF180A0A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Comical Crash Header
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "CRASHED!",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFF1744)
                )
                Text(
                    text = "RUN COMPLETED",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    letterSpacing = 2.sp
                )
            }

            // Stat Summary Panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1414)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "FINAL SCORE", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = "$score", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("DISTANCE", color = Color.LightGray, fontSize = 11.sp)
                            Text("${dist}m", color = Color(0xFF00E5FF), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("COINS", color = Color.LightGray, fontSize = 11.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = if (doubleCoinsSuccess) "${coins * 2} (x2!)" else "$coins",
                                    color = Color(0xFFFFD700),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Monetization AD-Reward Flow (Watch Ad to Double Coins)
            if (!doubleCoinsSuccess) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFFFEB3B), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3E2723)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(imageVector = Icons.Default.OndemandVideo, contentDescription = "Ad", tint = Color(0xFFFFEB3B), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Double Your Coins!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Watch a quick 3s sponsor video", color = Color.LightGray, fontSize = 10.sp)
                            }
                        }
                        Button(
                            onClick = { adWatching = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("WATCH AD", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2E7D32), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Double Coins applied! +${coins} bonus coins granted.", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { viewModel.startGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RUN AGAIN", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { viewModel.navigateTo(Screen.MAIN_MENU) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("MAIN MENU", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Animated overlay for Simulated Advertisement
        if (adWatching) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.OndemandVideo, contentDescription = null, tint = Color.White, modifier = Modifier.size(72.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("SPONSOR VIDEO PLAYING", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Simulated Google AdMob Video Stream", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Rewarding in $adTimer seconds...",
                        color = Color(0xFFFFC107),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

// ==========================================
// 8. HIGH SCORES (LEADERBOARD)
// ==========================================
@Composable
fun HighScoresScreen(viewModel: GameViewModel) {
    val topScores by viewModel.topScores.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.MAIN_MENU) }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LEADERBOARDS",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Leaderboard entries
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(topScores.take(8).sortedByDescending { it.score }) { run ->
                    val rank = topScores.indexOf(run) + 1
                    val rankColor = when (rank) {
                        1 -> Color(0xFFFFD700) // Gold
                        2 -> Color(0xFFC0C0C0) // Silver
                        3 -> Color(0xFFCD7F32) // Bronze
                        else -> Color.White.copy(alpha = 0.15f)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Trophy / Rank Badge
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(rankColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (rank <= 3) {
                                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    } else {
                                        Text("$rank", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Text(run.playerName, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("Dist: ${run.distance}m | Coins: ${run.coins}", color = Color.Gray, fontSize = 11.sp)
                                }
                            }

                            Text(
                                text = "${run.score}",
                                color = if (rank == 1) Color(0xFFFFD700) else Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 9. ACHIEVEMENTS SCREEN
// ==========================================
@Composable
fun AchievementsScreen(viewModel: GameViewModel) {
    val achievements by viewModel.achievements.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.MAIN_MENU) }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ACHIEVEMENTS",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Achievement List
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(achievements) { ach ->
                    val progressRatio = ach.progress.toFloat() / ach.maxProgress.toFloat()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Medal icon
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(if (ach.isUnlocked) Color(0xFFFFD700) else Color.DarkGray, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (ach.isUnlocked) Icons.Default.Star else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (ach.isUnlocked) Color.Black else Color.LightGray,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ach.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = ach.description,
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Progress row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = { progressRatio },
                                        color = Color(0xFF4CAF50),
                                        trackColor = Color.White.copy(alpha = 0.1f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                    )
                                    Text(
                                        text = "${ach.progress}/${ach.maxProgress}",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Claim Reward Token
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                Text("+${ach.rewardCoins}", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 10. DAILY REWARDS CALENDAR
// ==========================================
@Composable
fun DailyRewardsScreen(viewModel: GameViewModel, profile: UserProfile) {
    val statusMessage by viewModel.dailyClaimStatusMessage.collectAsState()
    val now = System.currentTimeMillis()
    val oneDayMillis = 24 * 60 * 60 * 1000
    val canClaim = now - profile.lastDailyRewardClaimedTime >= oneDayMillis

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.navigateTo(Screen.MAIN_MENU) }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DAILY REWARDS",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Hero Chest Graphic
                Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = if (canClaim) Color(0xFFFFC107) else Color.Gray,
                    modifier = Modifier
                        .size(120.dp)
                        .shadow(if (canClaim) 15.dp else 0.dp, CircleShape)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (canClaim) "YOUR DAILY CHEST IS READY!" else "DAILY REWARD CLAIMED",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (canClaim) "Unlock the chest below to claim 250 free gold coins!" else "Check back in 24 hours for your next reward chest.",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Grid layout of weekly claims
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (day in 1..5) {
                        val isCurrent = day == 1 // highlight first chest
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.8f)
                                .background(
                                    color = if (isCurrent && canClaim) Color(0xFF2E7D32) else Color(0xFF1E293B),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = if (isCurrent) 2.dp else 1.dp,
                                    color = if (isCurrent) Color(0xFFFFC107) else Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("DAY $day", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Icon(
                                    imageVector = Icons.Default.CardGiftcard,
                                    contentDescription = null,
                                    tint = if (isCurrent && canClaim) Color(0xFFFFD700) else Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("+250", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Claim trigger
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Display feedback status
                if (statusMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = statusMessage!!,
                            color = if (statusMessage!!.startsWith("SUCCESS")) Color.Green else Color.Red,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = { viewModel.claimDailyReward() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canClaim) Color(0xFF4CAF50) else Color.Gray
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = canClaim,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("CLAIM CHEST (+250 COINS)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
