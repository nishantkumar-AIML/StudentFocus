package com.ai_assistant.studentfocus.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Biometric lock screen.
 * - NO biometric API calls here — zero risk of double-prompt.
 * - MainActivity triggers the initial prompt via lifecycleScope.
 * - "Try again" / fingerprint button calls [onTriggerBiometric] back to MainActivity.
 */
@Composable
fun PinLockScreen(
    correctPin: String,             // kept for API compat — unused
    onUnlocked: () -> Unit,
    onTriggerBiometric: () -> Unit = {}
) {
    // Pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "fp_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fpScale"
    )

    // Sweeping laser scanner animation
    val scannerTransition = rememberInfiniteTransition(label = "scanner")
    val sweepOffset by scannerTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sweep"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF030712), Color(0xFF0F172A), Color(0xFF030712))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Futuristic floating ambient background orbs
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.TopStart)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0x138B5CF6), Color.Transparent)
                        ),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.BottomEnd)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0x1306B6D4), Color.Transparent)
                        ),
                        CircleShape
                    )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(
                    id = com.ai_assistant.studentfocus.R.drawable.app_logo
                ),
                contentDescription = "StudentOS Logo",
                modifier = Modifier.size(92.dp)
            )

            Text(
                text = "StudentOS",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "SYSTEM ACCESS LOCKED",
                color = Color(0xFF6366F1),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Fingerprint target with Laser Sweep Scanner
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale),
                contentAlignment = Alignment.Center
            ) {
                // outer glow ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF6366F1).copy(alpha = 0.2f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )

                // Biometric target button
                Button(
                    onClick = { onTriggerBiometric() },
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                width = 2.dp,
                                brush = Brush.sweepGradient(
                                    colors = listOf(Color(0xFF6366F1), Color(0xFFEC4899), Color(0xFF06B6D4), Color(0xFF6366F1))
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🫆", fontSize = 48.sp, textAlign = TextAlign.Center)
                    }
                }

                // Sweeping laser scanner line
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(2.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = (10 + sweepOffset).dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF06B6D4), Color(0xFF22C55E), Color(0xFF06B6D4), Color.Transparent)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Verify identity to unlock workspace",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            TextButton(
                onClick = { onTriggerBiometric() },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "Tap to authorize biometric scan",
                    color = Color(0xFF818CF8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
