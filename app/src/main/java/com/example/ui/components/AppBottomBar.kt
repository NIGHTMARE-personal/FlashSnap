package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.Screen

@Composable
fun AppBottomBar(
    currentScreen: Screen,
    onTabSelected: (Screen) -> Unit,
    onCameraShutterClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isCameraMode = currentScreen is Screen.Camera

    // Outer Box ensures the hovered center camera button is NOT clipped at the top
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. Sleek Dock Bar Surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 1: Pages (Scanned Pages & Notes)
                CompactNavItem(
                    icon = Icons.Filled.Description,
                    label = "Pages",
                    isSelected = currentScreen is Screen.Pages,
                    testTag = "bottom_tab_pages",
                    onClick = { onTabSelected(Screen.Pages) }
                )

                // Tab 2: Decks (Flashcards)
                CompactNavItem(
                    icon = Icons.Filled.Style,
                    label = "Decks",
                    isSelected = currentScreen is Screen.Decks,
                    testTag = "bottom_tab_decks",
                    onClick = { onTabSelected(Screen.Decks) }
                )

                // Space reserved for the big hovered camera shutter button in the center
                Spacer(modifier = Modifier.width(74.dp))

                // Tab 4: Quiz (Negative Marking & Duolingo Cards)
                CompactNavItem(
                    icon = Icons.Filled.School,
                    label = "Quiz",
                    isSelected = currentScreen is Screen.QuizSelect || currentScreen is Screen.Quiz || currentScreen is Screen.QuizResult || currentScreen is Screen.QuizDashboard,
                    testTag = "bottom_tab_quiz",
                    onClick = { onTabSelected(Screen.QuizSelect) }
                )

                // Tab 5: Settings & Profile
                CompactNavItem(
                    icon = Icons.Filled.Settings,
                    label = "Settings",
                    isSelected = currentScreen is Screen.Settings,
                    testTag = "bottom_tab_settings",
                    onClick = { onTabSelected(Screen.Settings) }
                )
            }
        }

        // 2. Hovered Center Camera Button with Iconic Shutter Ring
        // Dual Mode: If on camera screen -> clicks picture! If on other pages -> opens camera screen.
        HoveredShutterCameraTab(
            isCameraMode = isCameraMode,
            onClick = {
                if (isCameraMode) {
                    onCameraShutterClick()
                } else {
                    onTabSelected(Screen.Camera)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .offset(y = (-18).dp) // Hovers cleanly above the dock without clipping
        )
    }
}

/**
 * Hovered Center Camera Button with Camera Shutter Ring
 * Displays the distinctive outer ring, tactile push feedback, and dual-mode behavior.
 */
@Composable
private fun HoveredShutterCameraTab(
    isCameraMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        label = "shutter_press_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        // Outer Shutter Ring Container (68dp)
        Box(
            modifier = Modifier
                .size(68.dp)
                .scale(animatedScale)
                .shadow(
                    elevation = if (isCameraMode) 8.dp else 5.dp,
                    shape = CircleShape,
                    ambientColor = if (isCameraMode) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.3f),
                    spotColor = if (isCameraMode) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.4f)
                )
                .clip(CircleShape)
                // Outer ring styling with translucent gap
                .background(
                    if (isCameraMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                    else MaterialTheme.colorScheme.surface
                )
                .border(
                    width = 3.5.dp,
                    color = if (isCameraMode) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = CircleShape
                )
                .padding(4.5.dp) // The gap between outer ring and inner shutter button
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    onClick()
                }
                .testTag("bottom_tab_camera"),
            contentAlignment = Alignment.Center
        ) {
            // Inner Core Button
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCameraMode) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isCameraMode) Color.White.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = if (isCameraMode) "Click Picture" else "Open Camera",
                    tint = if (isCameraMode) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = if (isCameraMode) "Snap" else "Camera",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isCameraMode) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

/**
 * Compact Nav Item for Side Tabs with sleek, animated pill indicator
 */
@Composable
private fun CompactNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        label = "nav_item_scale"
    )

    val pillWidth by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isSelected) 44.dp else 28.dp,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "nav_pill_width"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
        ),
        label = "nav_icon_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .scale(animatedScale)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .testTag(testTag)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .height(28.dp)
                .width(pillWidth)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier
                    .size(20.dp)
                    .scale(iconScale)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            fontSize = 10.sp
        )
    }
}
