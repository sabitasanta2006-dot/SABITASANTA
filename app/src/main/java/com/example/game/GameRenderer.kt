package com.example.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import com.example.game.GameEngine.Companion.FLOOR_Y
import com.example.game.GameEngine.Companion.RUNNER_HEIGHT
import com.example.game.GameEngine.Companion.RUNNER_WIDTH
import com.example.game.GameEngine.Companion.RUNNER_X
import com.example.game.GameEngine.Companion.SLIDE_HEIGHT
import com.example.game.GameEngine.Companion.VIRTUAL_HEIGHT
import com.example.game.GameEngine.Companion.VIRTUAL_WIDTH
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameCanvas(
    game: GameEngine,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() }
                )
            }
    ) {
        val scaleX = size.width / VIRTUAL_WIDTH
        val scaleY = size.height / VIRTUAL_HEIGHT

        // Draw the full virtual game using dynamic scaling
        withTransform({
            scale(scaleX, scaleY, pivot = Offset.Zero)
        }) {
            drawSkyBackground(game)
            drawParallaxMountains(game)
            drawParallaxClouds(game)
            drawFloorGround(game)
            
            // Draw Interactive Game Objects
            drawCoins(game)
            drawPowerUps(game)
            drawObstacles(game)
            drawParticles(game)
            
            // Draw Character
            drawRunner(game)
        }
    }
}

// --- Procedural Sky Background with Day & Night Blending ---
private fun DrawScope.drawSkyBackground(game: GameEngine) {
    val cycle = game.dayNightCycle

    // Interpolate Sky Colors:
    // 0.0 - Noon (Light Sky Blue to Cyan)
    // 0.25 - Sunset (Coral/Crimson to Orange)
    // 0.5 - Night (Deep Navy to Black with Stars)
    // 0.75 - Sunrise (Magenta to Golden Yellow)
    val (skyTop, skyBottom) = when {
        cycle < 0.25f -> {
            val t = cycle / 0.25f
            val top = lerpColor(Color(0xFF87CEEB), Color(0xFFFF5722), t)
            val bottom = lerpColor(Color(0xFFE0F7FA), Color(0xFFFF9800), t)
            top to bottom
        }
        cycle < 0.5f -> {
            val t = (cycle - 0.25f) / 0.25f
            val top = lerpColor(Color(0xFFFF5722), Color(0xFF0D0D26), t)
            val bottom = lerpColor(Color(0xFFFF9800), Color(0xFF030308), t)
            top to bottom
        }
        cycle < 0.75f -> {
            val t = (cycle - 0.5f) / 0.25f
            val top = lerpColor(Color(0xFF0D0D26), Color(0xFFE040FB), t)
            val bottom = lerpColor(Color(0xFF030308), Color(0xFFFFD700), t)
            top to bottom
        }
        else -> {
            val t = (cycle - 0.75f) / 0.25f
            val top = lerpColor(Color(0xFFE040FB), Color(0xFF87CEEB), t)
            val bottom = lerpColor(Color(0xFFFFD700), Color(0xFFE0F7FA), t)
            top to bottom
        }
    }

    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(skyTop, skyBottom),
            start = Offset(0f, 0f),
            end = Offset(0f, VIRTUAL_HEIGHT)
        ),
        size = Size(VIRTUAL_WIDTH, VIRTUAL_HEIGHT)
    )

    // Twinkling stars at Night (when cycle is close to 0.5)
    if (cycle > 0.4f && cycle < 0.6f) {
        val nightIntensity = 1f - (Math.abs(cycle - 0.5f) / 0.1f)
        val starSeed = 42L
        val random = java.util.Random(starSeed)
        
        for (i in 0 until 50) {
            val sx = random.nextFloat() * VIRTUAL_WIDTH
            val sy = random.nextFloat() * (FLOOR_Y - 200f)
            val starAlpha = (0.2f + 0.8f * sin((game.gameTime * 0.05f) + i)) * nightIntensity
            val starSize = (1f + random.nextFloat() * 4f)
            
            drawCircle(
                color = Color.White.copy(alpha = starAlpha.coerceIn(0f, 1f)),
                radius = starSize,
                center = Offset(sx, sy)
            )
        }
    }
}

// --- Parallax Mountains (Distant Background) ---
private fun DrawScope.drawParallaxMountains(game: GameEngine) {
    val offset = game.bgOffsetFar
    val color = lerpMountainColor(game.dayNightCycle)

    // Two layered mountain paths to create parallax depth
    drawMountainRange(offset, heightFactor = 400f, peakSpacing = 600f, color = color.copy(alpha = 0.4f), yShift = 100f)
    drawMountainRange((offset * 1.5f) % VIRTUAL_WIDTH, heightFactor = 250f, peakSpacing = 400f, color = color, yShift = 0f)
}

private fun DrawScope.drawMountainRange(offset: Float, heightFactor: Float, peakSpacing: Float, color: Color, yShift: Float) {
    val path = Path()
    path.moveTo(-offset, FLOOR_Y)

    var currentX = -offset
    var index = 0
    while (currentX < VIRTUAL_WIDTH + peakSpacing) {
        val peakY = FLOOR_Y - 100f - yShift - (if (index % 2 == 0) heightFactor else heightFactor * 0.7f)
        path.quadraticTo(
            currentX + peakSpacing / 2f, peakY,
            currentX + peakSpacing, FLOOR_Y
        )
        currentX += peakSpacing
        index++
    }
    path.lineTo(VIRTUAL_WIDTH, FLOOR_Y)
    path.lineTo(0f, FLOOR_Y)
    path.close()

    drawPath(path = path, color = color)
}

// --- Parallax Clouds (Mid-ground) ---
private fun DrawScope.drawParallaxClouds(game: GameEngine) {
    val offset = game.bgOffsetMid
    val cloudColor = Color.White.copy(alpha = if (game.dayNightCycle > 0.4f && game.dayNightCycle < 0.6f) 0.15f else 0.5f)

    val cloudsData = listOf(
        Offset(300f, 150f) to 80f,
        Offset(800f, 220f) to 60f,
        Offset(1400f, 100f) to 100f,
        Offset(1900f, 250f) to 70f
    )

    for (cloud in cloudsData) {
        val basePos = cloud.first
        val r = cloud.second
        var cx = basePos.x - offset
        if (cx < -r * 3) cx += VIRTUAL_WIDTH + r * 3

        // Draw compound cloud shapes (fluffy arcs)
        drawCircle(color = cloudColor, radius = r, center = Offset(cx, basePos.y))
        drawCircle(color = cloudColor, radius = r * 0.8f, center = Offset(cx - r * 0.6f, basePos.y + r * 0.1f))
        drawCircle(color = cloudColor, radius = r * 0.7f, center = Offset(cx + r * 0.6f, basePos.y + r * 0.1f))
    }
}

// --- Foreground Ground Layer (Scrolling) ---
private fun DrawScope.drawFloorGround(game: GameEngine) {
    // Dynamic ground color changes depending on time of day
    val groundColor = when {
        game.dayNightCycle < 0.25f -> Color(0xFF4CAF50) // Bright Green
        game.dayNightCycle < 0.5f -> Color(0xFF2E7D32) // Forest Green (Dusk)
        game.dayNightCycle < 0.75f -> Color(0xFF1B5E20) // Deep Dark Green (Night)
        else -> Color(0xFF81C784) // Warm Soft Green (Morning)
    }
    val soilColor = Color(0xFF795548) // Brown soil

    // Draw bottom soil block
    drawRect(
        color = soilColor,
        topLeft = Offset(0f, FLOOR_Y),
        size = Size(VIRTUAL_WIDTH, VIRTUAL_HEIGHT - FLOOR_Y)
    )

    // Draw grass top border
    drawRect(
        color = groundColor,
        topLeft = Offset(0f, FLOOR_Y),
        size = Size(VIRTUAL_WIDTH, 20f)
    )

    // Draw scrolling ground highlights to show motion
    val offset = game.bgOffsetClose
    var currentX = -offset
    while (currentX < VIRTUAL_WIDTH + 100f) {
        // Draw grass blades or soil spots
        drawRect(
            color = groundColor.copy(alpha = 0.5f),
            topLeft = Offset(currentX, FLOOR_Y + 25f),
            size = Size(40f, 6f)
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.15f),
            topLeft = Offset(currentX + 50f, FLOOR_Y + 80f),
            size = Size(20f, 12f)
        )
        currentX += 160f
    }
}

// --- Interactive Items: Coins with Shimmering 3D Spins ---
private fun DrawScope.drawCoins(game: GameEngine) {
    for (coin in game.coins) {
        if (coin.isCollected) continue

        // Shimmering 3D Spin effect by compressing width using a sin wave
        val spinFactor = sin((game.gameTime * 0.15f) + coin.x * 0.01f)
        val radiusX = coin.radius * Math.abs(spinFactor).coerceAtLeast(0.15f)
        val radiusY = coin.radius

        val coinColor = if (coin.isDouble) Color(0xFFFF9800) else Color(0xFFFFD700)
        val innerColor = if (coin.isDouble) Color(0xFFFFEB3B) else Color(0xFFFFF9C4)

        // Outer Ring
        drawOval(
            color = coinColor,
            topLeft = Offset(coin.x - radiusX, coin.y - radiusY),
            size = Size(radiusX * 2f, radiusY * 2f)
        )

        // Inner Shimmer Center
        drawOval(
            color = innerColor,
            topLeft = Offset(coin.x - radiusX * 0.6f, coin.y - radiusY * 0.6f),
            size = Size(radiusX * 1.2f, radiusY * 1.2f)
        )

        // Draw a miniature 'C' or '$' in center
        if (radiusX > 5f) {
            drawCircle(
                color = coinColor,
                radius = radiusX * 0.2f,
                center = Offset(coin.x, coin.y)
            )
        }
    }
}

// --- Power-Up Canisters with Floating Glyphs ---
private fun DrawScope.drawPowerUps(game: GameEngine) {
    for (pu in game.powerUps) {
        if (pu.isCollected) continue

        // Hover bobbing effect
        val bob = sin((game.gameTime * 0.08f) + pu.x * 0.02f) * 15f
        val cy = pu.y + bob

        // Outer glow
        drawCircle(
            color = pu.type.color.copy(alpha = 0.3f),
            radius = pu.radius * 1.6f,
            center = Offset(pu.x, cy)
        )

        // Floating Shield/Canister Shape
        val path = Path()
        path.moveTo(pu.x, cy - pu.radius)
        path.lineTo(pu.x + pu.radius, cy - pu.radius * 0.5f)
        path.lineTo(pu.x + pu.radius, cy + pu.radius * 0.6f)
        path.lineTo(pu.x, cy + pu.radius)
        path.lineTo(pu.x - pu.radius, cy + pu.radius * 0.6f)
        path.lineTo(pu.x - pu.radius, cy - pu.radius * 0.5f)
        path.close()

        drawPath(path = path, color = pu.type.color)

        // Symbol indicators
        val symbol = when (pu.type) {
            PowerUpType.COIN_MAGNET -> "U"
            PowerUpType.SPEED_BOOST -> ">>"
            PowerUpType.SHIELD -> "O"
            PowerUpType.DOUBLE_COINS -> "2x"
            PowerUpType.EXTRA_LIFE -> "♥"
        }

        // Simulating text symbol drawing with simple procedural vector lines
        when (pu.type) {
            PowerUpType.EXTRA_LIFE -> { // Heart
                drawCircle(Color.White, radius = pu.radius * 0.3f, center = Offset(pu.x - pu.radius * 0.25f, cy - pu.radius * 0.1f))
                drawCircle(Color.White, radius = pu.radius * 0.3f, center = Offset(pu.x + pu.radius * 0.25f, cy - pu.radius * 0.1f))
                val heartPath = Path()
                heartPath.moveTo(pu.x - pu.radius * 0.55f, cy)
                heartPath.lineTo(pu.x + pu.radius * 0.55f, cy)
                heartPath.lineTo(pu.x, cy + pu.radius * 0.55f)
                heartPath.close()
                drawPath(heartPath, Color.White)
            }
            PowerUpType.SHIELD -> { // Shield Circle
                drawCircle(
                    color = Color.White,
                    radius = pu.radius * 0.4f,
                    center = Offset(pu.x, cy),
                    style = Stroke(width = 4f)
                )
            }
            PowerUpType.COIN_MAGNET -> { // U magnet
                drawArc(
                    color = Color.White,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(pu.x - pu.radius * 0.4f, cy - pu.radius * 0.2f),
                    size = Size(pu.radius * 0.8f, pu.radius * 0.6f),
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
            }
            PowerUpType.SPEED_BOOST -> { // Chevrons >>
                val p1 = Path().apply {
                    moveTo(pu.x - pu.radius * 0.3f, cy - pu.radius * 0.3f)
                    lineTo(pu.x + pu.radius * 0.1f, cy)
                    lineTo(pu.x - pu.radius * 0.3f, cy + pu.radius * 0.3f)
                }
                val p2 = Path().apply {
                    moveTo(pu.x + pu.radius * 0.1f, cy - pu.radius * 0.3f)
                    lineTo(pu.x + pu.radius * 0.5f, cy)
                    lineTo(pu.x + pu.radius * 0.1f, cy + pu.radius * 0.3f)
                }
                drawPath(p1, Color.White, style = Stroke(width = 5f, join = StrokeJoin.Round))
                drawPath(p2, Color.White, style = Stroke(width = 5f, join = StrokeJoin.Round))
            }
            PowerUpType.DOUBLE_COINS -> { // Gold star representation
                drawStar(pu.x, cy, pu.radius * 0.45f, Color.White)
            }
        }
    }
}

// --- Detailed Custom Vectors for Hazards & Moving Obstacles ---
private fun DrawScope.drawObstacles(game: GameEngine) {
    for (obs in game.obstacles) {
        when (obs.type) {
            ObstacleType.SPIKES -> {
                // Classic dangerous triangular teeth
                val spikeColor = Color(0xFF616161)
                val spikeHighlight = Color(0xFFBDBDBD)
                val numTeeth = 3
                val toothWidth = obs.width / numTeeth

                for (i in 0 until numTeeth) {
                    val toothX = obs.x + (i * toothWidth)
                    val path = Path()
                    path.moveTo(toothX, obs.y + obs.height)
                    path.lineTo(toothX + toothWidth / 2f, obs.y)
                    path.lineTo(toothX + toothWidth, obs.y + obs.height)
                    path.close()
                    drawPath(path = path, color = spikeColor)

                    // Draw inner highlighting on left side of spike
                    val hlPath = Path()
                    hlPath.moveTo(toothX, obs.y + obs.height)
                    hlPath.lineTo(toothX + toothWidth / 2f, obs.y)
                    hlPath.lineTo(toothX + toothWidth / 2f, obs.y + obs.height)
                    hlPath.close()
                    drawPath(hlPath, spikeHighlight)
                }
            }
            ObstacleType.FLYING_BIRD -> {
                // Cartoon bird: flapping wings, red head, yellow beak
                val state = obs.stateTime
                // Wing flap rotation
                val wingFlap = sin(state * 0.4f) * 35f // Flapping angle in degrees

                // Body (Oval)
                drawOval(
                    color = Color(0xFFF44336), // Red Angry Bird Style
                    topLeft = Offset(obs.x, obs.y + obs.height * 0.1f),
                    size = Size(obs.width * 0.85f, obs.height * 0.75f)
                )

                // Beak (Triangle)
                val beakPath = Path()
                beakPath.moveTo(obs.x, obs.y + obs.height * 0.45f)
                beakPath.lineTo(obs.x - obs.width * 0.35f, obs.y + obs.height * 0.55f)
                beakPath.lineTo(obs.x, obs.y + obs.height * 0.65f)
                beakPath.close()
                drawPath(beakPath, Color(0xFFFFEB3B)) // Yellow beak

                // Eye
                drawCircle(Color.White, radius = obs.width * 0.12f, center = Offset(obs.x + obs.width * 0.25f, obs.y + obs.height * 0.35f))
                drawCircle(Color.Black, radius = obs.width * 0.05f, center = Offset(obs.x + obs.width * 0.2f, obs.y + obs.height * 0.35f))

                // Wing flapping (Rotated vector lines / shapes)
                withTransform({
                    translate(obs.x + obs.width * 0.45f, obs.y + obs.height * 0.45f)
                    rotate(wingFlap)
                }) {
                    val wingPath = Path()
                    wingPath.moveTo(0f, 0f)
                    wingPath.lineTo(obs.width * 0.1f, -obs.height * 0.6f)
                    wingPath.lineTo(-obs.width * 0.25f, -obs.height * 0.5f)
                    wingPath.close()
                    drawPath(wingPath, Color(0xFFB71C1C)) // Darker red wing
                }
            }
            ObstacleType.ROBOT -> {
                // Industrial metallic robotic threat with rolling treads and laser scanners
                val frame = (game.gameTime / 6) % 2
                
                // Metal feet / treads (changes height slightly to look like walking/rolling)
                drawRoundRect(
                    color = Color(0xFF455A64),
                    topLeft = Offset(obs.x + obs.width * 0.1f, obs.y + obs.height * 0.8f),
                    size = Size(obs.width * 0.8f, obs.height * 0.2f),
                    cornerRadius = CornerRadius(10f)
                )

                // Main body
                drawRoundRect(
                    color = Color(0xFF78909C),
                    topLeft = Offset(obs.x + obs.width * 0.15f, obs.y + obs.height * 0.25f),
                    size = Size(obs.width * 0.7f, obs.height * 0.6f),
                    cornerRadius = CornerRadius(15f),
                    style = Fill
                )

                // Core outline
                drawRoundRect(
                    color = Color(0xFF37474F),
                    topLeft = Offset(obs.x + obs.width * 0.15f, obs.y + obs.height * 0.25f),
                    size = Size(obs.width * 0.7f, obs.height * 0.6f),
                    cornerRadius = CornerRadius(15f),
                    style = Stroke(width = 4f)
                )

                // Laser eye visor (flashes)
                val visorColor = if (frame == 0L) Color(0xFFFF1744) else Color(0xFFB71C1C)
                drawRect(
                    color = visorColor,
                    topLeft = Offset(obs.x + obs.width * 0.3f, obs.y + obs.height * 0.4f),
                    size = Size(obs.width * 0.4f, obs.height * 0.1f)
                )
                
                // Scanning grid glow
                drawRect(
                    color = Color.Black.copy(alpha = 0.2f),
                    topLeft = Offset(obs.x + obs.width * 0.25f, obs.y + obs.height * 0.55f),
                    size = Size(obs.width * 0.5f, obs.height * 0.2f)
                )
            }
            ObstacleType.GHOST -> {
                // Eerie hovering specter
                val hoverAlpha = 0.55f + 0.25f * sin(game.gameTime * 0.1f)
                val specterColor = Color(0xFFE040FB).copy(alpha = hoverAlpha)

                val ghostPath = Path()
                ghostPath.moveTo(obs.x + obs.width * 0.5f, obs.y)
                
                // Rounded top
                ghostPath.quadraticTo(obs.x + obs.width, obs.y, obs.x + obs.width, obs.y + obs.height * 0.5f)
                
                // Slithering spikes on bottom of ghost
                val spikeOffset = sin(game.gameTime * 0.15f) * 12f
                ghostPath.lineTo(obs.x + obs.width, obs.y + obs.height * 0.85f)
                ghostPath.lineTo(obs.x + obs.width * 0.8f, obs.y + obs.height * 0.75f + spikeOffset)
                ghostPath.lineTo(obs.x + obs.width * 0.6f, obs.y + obs.height * 0.85f - spikeOffset)
                ghostPath.lineTo(obs.x + obs.width * 0.4f, obs.y + obs.height * 0.75f + spikeOffset)
                ghostPath.lineTo(obs.x + obs.width * 0.2f, obs.y + obs.height * 0.85f - spikeOffset)
                ghostPath.lineTo(obs.x, obs.y + obs.height * 0.85f)
                
                ghostPath.quadraticTo(obs.x, obs.y, obs.x + obs.width * 0.5f, obs.y)
                ghostPath.close()

                drawPath(ghostPath, specterColor)

                // Glowing blank eyes
                drawCircle(Color.White, radius = obs.width * 0.1f, center = Offset(obs.x + obs.width * 0.35f, obs.y + obs.height * 0.35f))
                drawCircle(Color.White, radius = obs.width * 0.1f, center = Offset(obs.x + obs.width * 0.65f, obs.y + obs.height * 0.35f))
                drawCircle(Color(0xFF311B92), radius = obs.width * 0.04f, center = Offset(obs.x + obs.width * 0.35f, obs.y + obs.height * 0.35f))
                drawCircle(Color(0xFF311B92), radius = obs.width * 0.04f, center = Offset(obs.x + obs.width * 0.65f, obs.y + obs.height * 0.35f))
            }
            ObstacleType.ROLLING_STONE -> {
                // Spinning crushing boulder
                val rotationAngle = (game.gameTime * 5f) % 360f

                withTransform({
                    translate(obs.x + obs.width / 2f, obs.y + obs.height / 2f)
                    rotate(rotationAngle)
                }) {
                    // Draw outer rock circle
                    drawCircle(
                        color = Color(0xFF757575),
                        radius = obs.width / 2f
                    )

                    // Draw stone cracks and texture details
                    drawLine(
                        color = Color(0xFF424242),
                        start = Offset(-obs.width * 0.4f, -obs.height * 0.1f),
                        end = Offset(obs.width * 0.3f, obs.height * 0.2f),
                        strokeWidth = 6f
                    )
                    drawLine(
                        color = Color(0xFF424242),
                        start = Offset(-obs.width * 0.1f, -obs.height * 0.4f),
                        end = Offset(-obs.width * 0.2f, obs.height * 0.3f),
                        strokeWidth = 5f
                    )
                    drawLine(
                        color = Color(0xFF424242),
                        start = Offset(0f, 0f),
                        end = Offset(obs.width * 0.4f, -obs.height * 0.3f),
                        strokeWidth = 4f
                    )

                    // Outermost shadow border
                    drawCircle(
                        color = Color(0xFF212121),
                        radius = obs.width / 2f,
                        style = Stroke(width = 6f)
                    )
                }
            }
            ObstacleType.FIRE_TRAP -> {
                // Steam nozzle that vents flames upwards
                val isActiveFlame = (game.gameTime / 30) % 2 == 0L

                // Base nozzle
                drawRoundRect(
                    color = Color(0xFF37474F),
                    topLeft = Offset(obs.x + obs.width * 0.15f, obs.y + obs.height * 0.6f),
                    size = Size(obs.width * 0.7f, obs.height * 0.4f),
                    cornerRadius = CornerRadius(8f)
                )
                drawRect(
                    color = Color(0xFF263238),
                    topLeft = Offset(obs.x + obs.width * 0.35f, obs.y + obs.height * 0.4f),
                    size = Size(obs.width * 0.3f, obs.height * 0.2f)
                )

                // Burning dangerous flames
                if (isActiveFlame) {
                    val randHeight = 60f + sin(game.gameTime * 0.5f) * 15f
                    val fPath = Path()
                    fPath.moveTo(obs.x + obs.width * 0.3f, obs.y + obs.height * 0.4f)
                    fPath.quadraticTo(obs.x + obs.width * 0.5f, obs.y + obs.height * 0.4f - randHeight, obs.x + obs.width * 0.5f, obs.y - 15f)
                    fPath.quadraticTo(obs.x + obs.width * 0.5f, obs.y + obs.height * 0.4f - randHeight, obs.x + obs.width * 0.7f, obs.y + obs.height * 0.4f)
                    fPath.close()

                    // Yellow inner flame core
                    val fCorePath = Path()
                    fCorePath.moveTo(obs.x + obs.width * 0.4f, obs.y + obs.height * 0.4f)
                    fCorePath.quadraticTo(obs.x + obs.width * 0.5f, obs.y + obs.height * 0.4f - randHeight * 0.6f, obs.x + obs.width * 0.5f, obs.y + 15f)
                    fCorePath.quadraticTo(obs.x + obs.width * 0.5f, obs.y + obs.height * 0.4f - randHeight * 0.6f, obs.x + obs.width * 0.6f, obs.y + obs.height * 0.4f)
                    fCorePath.close()

                    drawPath(fPath, Brush.verticalGradient(listOf(Color(0xFFFF1744), Color(0xFFFF9100))))
                    drawPath(fCorePath, Brush.verticalGradient(listOf(Color(0xFFFFEA00), Color(0xFFFF9100))))
                }
            }
            ObstacleType.FALLING_PLATFORM -> {
                // Elegant rustic floating timber platform
                drawRoundRect(
                    color = Color(0xFF8D6E63),
                    topLeft = Offset(obs.x, obs.y),
                    size = Size(obs.width, obs.height),
                    cornerRadius = CornerRadius(10f)
                )

                // Wood grain lines
                drawLine(
                    color = Color(0xFF5D4037),
                    start = Offset(obs.x + 15f, obs.y + obs.height * 0.3f),
                    end = Offset(obs.x + obs.width - 15f, obs.y + obs.height * 0.3f),
                    strokeWidth = 3f
                )
                drawLine(
                    color = Color(0xFF5D4037),
                    start = Offset(obs.x + 30f, obs.y + obs.height * 0.7f),
                    end = Offset(obs.x + obs.width - 45f, obs.y + obs.height * 0.7f),
                    strokeWidth = 3f
                )

                // Dark Border
                drawRoundRect(
                    color = Color(0xFF4E342E),
                    topLeft = Offset(obs.x, obs.y),
                    size = Size(obs.width, obs.height),
                    cornerRadius = CornerRadius(10f),
                    style = Stroke(width = 4f)
                )
            }
        }
    }
}

// --- Dynamic Particle Physics Renderer ---
private fun DrawScope.drawParticles(game: GameEngine) {
    for (p in game.particles) {
        drawCircle(
            color = p.color.copy(alpha = p.color.alpha * p.life),
            radius = p.radius * p.life,
            center = Offset(p.x, p.y)
        )
    }
}

// --- Procedural Vectors & Animation Keyframes for the Runner ---
private fun DrawScope.drawRunner(game: GameEngine) {
    if (game.runnerState == CharacterState.DEAD) {
        // Render comical horizontal knockout drawing
        drawDeadRunner(game)
        return
    }

    val state = game.runnerState
    val pColor = game.characterConfig.primaryColor
    val aColor = game.characterConfig.accentColor

    // Dynamic scale/translate context based on State
    val isSliding = state == CharacterState.SLIDING
    val ry = if (isSliding) game.runnerY + RUNNER_HEIGHT - SLIDE_HEIGHT else game.runnerY
    val rHeight = if (isSliding) SLIDE_HEIGHT else RUNNER_HEIGHT

    // Bouncing body offset based on running frames
    val runCycle = (game.gameTime * 0.25f)
    val bounceY = if (state == CharacterState.RUNNING) {
        Math.abs(sin(runCycle)) * 12f
    } else {
        0f
    }

    // Shield protective bubble rendering
    if (game.activePowerUps.containsKey(PowerUpType.SHIELD)) {
        val radius = RUNNER_WIDTH * 1.05f
        val shimmer = 0.8f + 0.2f * sin(game.gameTime * 0.15f)
        drawCircle(
            color = Color(0xFF03A9F4).copy(alpha = 0.2f * shimmer),
            radius = radius,
            center = Offset(RUNNER_X + RUNNER_WIDTH / 2f, ry + rHeight / 2f)
        )
        drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = 0.6f * shimmer),
            radius = radius,
            center = Offset(RUNNER_X + RUNNER_WIDTH / 2f, ry + rHeight / 2f),
            style = Stroke(width = 4f)
        )
    }

    // Magnet attract field visual feedback
    if (game.activePowerUps.containsKey(PowerUpType.COIN_MAGNET)) {
        val hasBonus = game.characterConfig.id == "star_knight"
        val maxRadius = if (hasBonus) 120f else 80f
        val pulse = (game.gameTime % 45) / 45f
        drawCircle(
            color = Color(0xFFE91E63).copy(alpha = 0.3f * (1f - pulse)),
            radius = maxRadius * pulse,
            center = Offset(RUNNER_X + RUNNER_WIDTH / 2f, ry + rHeight / 2f),
            style = Stroke(width = 3f)
        )
    }

    // Invulnerability flashing (skips drawing on alternating frames)
    if (game.isInvulnerable && (game.gameTime / 3) % 2 == 0L) {
        return
    }

    // Rotational transformation context for aerial flips (Double Jumps)
    val rollAngle = if (state == CharacterState.DOUBLE_JUMPING) {
        (game.gameTime * 15f) % 360f
    } else {
        0f
    }

    withTransform({
        translate(RUNNER_X + RUNNER_WIDTH / 2f, ry + rHeight / 2f)
        rotate(rollAngle)
    }) {
        val cx = -RUNNER_WIDTH / 2f
        val cy = -rHeight / 2f

        if (isSliding) {
            // SLIDE STATE: Flat rectangular layout
            // Body sliding capsule
            drawRoundRect(
                color = pColor,
                topLeft = Offset(cx, cy),
                size = Size(RUNNER_WIDTH, rHeight),
                cornerRadius = CornerRadius(20f)
            )
            // Cyber glass visor
            drawRoundRect(
                color = aColor,
                topLeft = Offset(cx + RUNNER_WIDTH * 0.55f, cy + rHeight * 0.2f),
                size = Size(RUNNER_WIDTH * 0.35f, rHeight * 0.4f),
                cornerRadius = CornerRadius(5f)
            )
            // Dust slide sparks
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = 12f,
                center = Offset(cx + 10f, cy + rHeight - 10f)
            )
        } else {
            // NORMAL / RUNNING / JUMPING: Fully articulated vector figure
            val torsoY = cy + bounceY
            val torsoH = rHeight * 0.45f
            val torsoW = RUNNER_WIDTH * 0.7f

            // 1. Legs (cycling oval strokes when running, tucked-up on jump)
            if (state == CharacterState.RUNNING) {
                // Leg 1 (Behind)
                val legCycle1 = runCycle
                val lx1 = cx + RUNNER_WIDTH * 0.35f + cos(legCycle1) * 30f
                val ly1 = cy + rHeight * 0.75f + sin(legCycle1) * 20f
                drawLine(
                    color = aColor,
                    start = Offset(cx + RUNNER_WIDTH * 0.35f, cy + rHeight * 0.5f + bounceY),
                    end = Offset(lx1, ly1),
                    strokeWidth = 14f,
                    cap = StrokeCap.Round
                )

                // Leg 2 (Front)
                val legCycle2 = runCycle + Math.PI.toFloat()
                val lx2 = cx + RUNNER_WIDTH * 0.45f + cos(legCycle2) * 30f
                val ly2 = cy + rHeight * 0.75f + sin(legCycle2) * 20f
                drawLine(
                    color = pColor,
                    start = Offset(cx + RUNNER_WIDTH * 0.45f, cy + rHeight * 0.5f + bounceY),
                    end = Offset(lx2, ly2),
                    strokeWidth = 14f,
                    cap = StrokeCap.Round
                )
            } else {
                // Jumping: Legs pulled/curled up
                drawLine(
                    color = pColor,
                    start = Offset(cx + RUNNER_WIDTH * 0.35f, cy + rHeight * 0.5f),
                    end = Offset(cx + RUNNER_WIDTH * 0.25f, cy + rHeight * 0.85f),
                    strokeWidth = 14f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = aColor,
                    start = Offset(cx + RUNNER_WIDTH * 0.5f, cy + rHeight * 0.5f),
                    end = Offset(cx + RUNNER_WIDTH * 0.65f, cy + rHeight * 0.85f),
                    strokeWidth = 14f,
                    cap = StrokeCap.Round
                )
            }

            // 2. Torso (Body jacket)
            drawRoundRect(
                color = pColor,
                topLeft = Offset(cx + RUNNER_WIDTH * 0.15f, torsoY + rHeight * 0.2f),
                size = Size(torsoW, torsoH),
                cornerRadius = CornerRadius(20f)
            )

            // 3. Head & Hair/Gear
            val headRadius = rHeight * 0.2f
            val headCenter = Offset(cx + RUNNER_WIDTH * 0.5f, torsoY + rHeight * 0.1f)
            drawCircle(
                color = Color(0xFFFDD835), // Golden skin color
                radius = headRadius,
                center = headCenter
            )

            // Hair/Cap (Styled based on accent color)
            val capPath = Path()
            capPath.moveTo(headCenter.x - headRadius * 1.1f, headCenter.y - headRadius * 0.3f)
            capPath.quadraticTo(headCenter.x - headRadius * 0.5f, headCenter.y - headRadius * 1.3f, headCenter.x + headRadius * 0.6f, headCenter.y - headRadius * 0.9f)
            capPath.lineTo(headCenter.x + headRadius * 1.2f, headCenter.y - headRadius * 0.3f)
            capPath.close()
            drawPath(capPath, aColor)

            // Dynamic futuristic Visor/Glasses on character face
            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(headCenter.x + headRadius * 0.1f, headCenter.y - headRadius * 0.4f),
                size = Size(headRadius * 1.1f, headRadius * 0.45f),
                cornerRadius = CornerRadius(5f)
            )
            drawRoundRect(
                color = aColor,
                topLeft = Offset(headCenter.x + headRadius * 0.2f, headCenter.y - headRadius * 0.35f),
                size = Size(headRadius * 0.9f, headRadius * 0.2f),
                cornerRadius = CornerRadius(2f)
            )

            // 4. Arms (Swinging lines)
            if (state == CharacterState.RUNNING) {
                // Arm swinging opposite to legs
                val armAngle = sin(runCycle) * 35f
                withTransform({
                    translate(cx + RUNNER_WIDTH * 0.4f, torsoY + rHeight * 0.3f)
                    rotate(armAngle)
                }) {
                    drawLine(
                        color = pColor,
                        start = Offset(0f, 0f),
                        end = Offset(0f, rHeight * 0.25f),
                        strokeWidth = 11f,
                        cap = StrokeCap.Round
                    )
                }
            } else {
                // JUMPING: Arms raised high!
                drawLine(
                    color = pColor,
                    start = Offset(cx + RUNNER_WIDTH * 0.4f, torsoY + rHeight * 0.3f),
                    end = Offset(cx + RUNNER_WIDTH * 0.75f, torsoY - rHeight * 0.05f),
                    strokeWidth = 11f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

// --- Knocked out Comical Static Drawing ---
private fun DrawScope.drawDeadRunner(game: GameEngine) {
    val ry = game.runnerY + RUNNER_HEIGHT - 40f
    val rx = RUNNER_X
    val pColor = game.characterConfig.primaryColor

    // Horizontal body capsule flat on ground
    drawRoundRect(
        color = pColor.copy(alpha = 0.7f),
        topLeft = Offset(rx, ry),
        size = Size(RUNNER_WIDTH * 1.1f, 40f),
        cornerRadius = CornerRadius(10f)
    )

    // Flat Head
    drawCircle(
        color = Color(0xFFFDD835).copy(alpha = 0.7f),
        radius = 22f,
        center = Offset(rx + RUNNER_WIDTH * 1.1f, ry + 15f)
    )

    // X-X Crossed Eyes
    drawLine(Color.Black, Offset(rx + RUNNER_WIDTH * 1.1f - 8f, ry + 10f), Offset(rx + RUNNER_WIDTH * 1.1f, ry + 18f), strokeWidth = 3f)
    drawLine(Color.Black, Offset(rx + RUNNER_WIDTH * 1.1f, ry + 10f), Offset(rx + RUNNER_WIDTH * 1.1f - 8f, ry + 18f), strokeWidth = 3f)
    drawLine(Color.Black, Offset(rx + RUNNER_WIDTH * 1.1f + 2f, ry + 10f), Offset(rx + RUNNER_WIDTH * 1.1f + 10f, ry + 18f), strokeWidth = 3f)
    drawLine(Color.Black, Offset(rx + RUNNER_WIDTH * 1.1f + 10f, ry + 10f), Offset(rx + RUNNER_WIDTH * 1.1f + 2f, ry + 18f), strokeWidth = 3f)

    // Halo Floating above head
    val haloBob = sin(game.gameTime * 0.1f) * 6f
    drawOval(
        color = Color(0xFFFFD700),
        topLeft = Offset(rx + RUNNER_WIDTH * 1.1f - 20f, ry - 30f + haloBob),
        size = Size(40f, 12f),
        style = Stroke(width = 4f)
    )
}

// --- Procedural Helpers ---
private fun lerpColor(c1: Color, c2: Color, t: Float): Color {
    val r = c1.red + (c2.red - c1.red) * t
    val g = c1.green + (c2.green - c1.green) * t
    val b = c1.blue + (c2.blue - c1.blue) * t
    val a = c1.alpha + (c2.alpha - c1.alpha) * t
    return Color(r, g, b, a)
}

private fun lerpMountainColor(cycle: Float): Color {
    return when {
        cycle < 0.25f -> { // Noon to Dusk (Green/Slate to Maroon/Sunset)
            val t = cycle / 0.25f
            lerpColor(Color(0xFF37474F), Color(0xFF4A148C), t)
        }
        cycle < 0.5f -> { // Dusk to Midnight (Maroon to Charcoal)
            val t = (cycle - 0.25f) / 0.25f
            lerpColor(Color(0xFF4A148C), Color(0xFF1A1A2E), t)
        }
        cycle < 0.75f -> { // Midnight to Dawn (Charcoal to Soft Purple)
            val t = (cycle - 0.5f) / 0.25f
            lerpColor(Color(0xFF1A1A2E), Color(0xFF3F51B5), t)
        }
        else -> { // Dawn to Noon (Soft Purple to Slate)
            val t = (cycle - 0.75f) / 0.25f
            lerpColor(Color(0xFF3F51B5), Color(0xFF37474F), t)
        }
    }
}

private fun DrawScope.drawStar(cx: Float, cy: Float, radius: Float, color: Color) {
    val path = Path()
    val numPoints = 5
    var angle = -Math.PI / 2.0
    val dAngle = Math.PI / numPoints

    for (i in 0 until numPoints * 2) {
        val r = if (i % 2 == 0) radius else radius * 0.45f
        val x = cx + (cos(angle) * r).toFloat()
        val y = cy + (sin(angle) * r).toFloat()
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
        angle += dAngle
    }
    path.close()
    drawPath(path, color)
}
