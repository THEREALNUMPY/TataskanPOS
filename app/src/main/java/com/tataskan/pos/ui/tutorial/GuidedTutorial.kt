package com.tataskan.pos.ui.tutorial

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tataskan.pos.util.Strings

data class TutorialStep(
    val id: String,
    val titleKey: String,
    val descKey: String,
    val alignment: Alignment = Alignment.Center,
    val requireAction: Boolean = false,
    val hasTarget: Boolean = true
)

@Composable
fun GuidedTutorialOverlay(
    activeStep: TutorialStep?,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    canSkip: Boolean,
    lang: String,
    targetBounds: Rect? = null
) {
    if (activeStep == null) return

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    
    var containerPos by remember { mutableStateOf(Offset.Zero) }
    val localTarget by remember(targetBounds, containerPos) {
        derivedStateOf { targetBounds?.translate(-containerPos) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { containerPos = it.positionInWindow() }
    ) {
        // 1. Darkened background with cut-out
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            drawRect(Color.Black.copy(alpha = 0.65f))
            
            localTarget?.let { bounds ->
                if (bounds.width > 0f && bounds.height > 0f) {
                    val margin = 10.dp.toPx()
                    val hole = Rect(
                        bounds.left - margin,
                        bounds.top - margin,
                        bounds.right + margin,
                        bounds.bottom + margin
                    )
                    drawRoundRect(
                        color = Color.Transparent, 
                        topLeft = hole.topLeft, 
                        size = hole.size, 
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f), 
                        blendMode = BlendMode.Clear
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.8f), 
                        topLeft = hole.topLeft, 
                        size = hole.size, 
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f), 
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
        }

        // 2. Interaction Blocker
        Box(modifier = Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { }))

        // 3. Content Layer (Inside safe areas)
        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            var safeAreaSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
            var safeAreaPos by remember { mutableStateOf(Offset.Zero) }
            Box(Modifier.fillMaxSize().onGloballyPositioned { 
                safeAreaSize = it.size
                safeAreaPos = it.positionInWindow()
            })

            val targetInSafe by remember(targetBounds, safeAreaPos) {
                derivedStateOf { targetBounds?.translate(-safeAreaPos) }
            }
            val isTargetPresent = targetInSafe != null && targetInSafe!!.width > 0f
            val target = targetInSafe ?: Rect.Zero
            val screenH = safeAreaSize.height.toFloat()
            
            val isTargetTop = if (isTargetPresent) target.center.y < screenH * 0.55f else false
            
            // Positioning Logic: Use a sub-container to place the card relative to the arrow
            val arrowSize = 36.dp
            val marginPx = with(density) { 10.dp.toPx() }
            val gapPx = with(density) { 6.dp.toPx() }
            val cardGapPx = with(density) { 12.dp.toPx() }

            val arrowTipY = if (isTargetTop) (target.bottom + marginPx + gapPx) 
                            else (target.top - marginPx - gapPx)
            val arrowBaseY = if (isTargetTop) (arrowTipY + with(density) { arrowSize.toPx() })
                             else (arrowTipY - with(density) { arrowSize.toPx() })

            // 4. Arrow
            if (isTargetPresent) {
                val icon = if (isTargetTop) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
                val y = if (isTargetTop) arrowTipY else arrowBaseY
                
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .offset(
                            x = with(density) { target.center.x.toDp() - 18.dp }.coerceIn(16.dp, (configuration.screenWidthDp - 52).dp),
                            y = with(density) { y.toDp() }
                        )
                        .size(arrowSize)
                )
            }

            // 5. Card - Positioned in a container that ends/starts at the arrow
            val dialogContainerModifier = if (isTargetPresent) {
                if (isTargetTop) {
                    // Target is at top, Box should be below arrow
                    Modifier
                        .padding(top = with(density) { (arrowBaseY + cardGapPx).toDp() })
                        .fillMaxSize()
                } else {
                    // Target is at bottom, Box should be above arrow
                    Modifier
                        .padding(bottom = with(density) { (screenH - arrowBaseY + cardGapPx).toDp() })
                        .fillMaxSize()
                }
            } else {
                Modifier.fillMaxSize()
            }

            Box(modifier = dialogContainerModifier) {
                AnimatedContent(
                    targetState = activeStep,
                    transitionSpec = { 
                        (fadeIn(tween(200)) + scaleIn(initialScale = 0.98f, animationSpec = tween(200)))
                            .togetherWith(fadeOut(tween(150))) 
                    },
                    modifier = Modifier
                        .align(if (isTargetPresent) (if (isTargetTop) Alignment.TopCenter else Alignment.BottomCenter) else activeStep.alignment)
                        .padding(16.dp)
                        .heightIn(max = if (isTargetPresent) {
                            if (isTargetTop) with(density) { (screenH - arrowBaseY - cardGapPx - 40f).coerceAtLeast(200f).toDp() }
                            else with(density) { (arrowBaseY - cardGapPx - 40f).coerceAtLeast(200f).toDp() }
                        } else 600.dp),
                    label = "tutCard"
                ) { step ->
                    if (step == null) return@AnimatedContent
                    ElevatedCard(
                        modifier = Modifier.widthIn(max = 350.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = Strings.get(step.titleKey, lang), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val fullDesc = Strings.get(step.descKey, lang)
                            val parts = remember(fullDesc) {
                                if (fullDesc.contains("Note:", ignoreCase = true)) fullDesc.split(Regex("(?i)Note:"), 2)
                                else if (fullDesc.contains("Paunawa:", ignoreCase = true)) fullDesc.split(Regex("(?i)Paunawa:"), 2)
                                else listOf(fullDesc)
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(text = parts[0].trim(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                if (parts.size > 1) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(text = parts[1].trim(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                if (canSkip) { TextButton(onClick = onSkip) { Text(Strings.get("tutorial_skip", lang), color = MaterialTheme.colorScheme.outline) } } else { Spacer(Modifier.width(1.dp)) }
                                Button(onClick = onNext, shape = RoundedCornerShape(12.dp), elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)) { Text(if (step.id == "final") "FINISH" else Strings.get("tutorial_next", lang).uppercase(), fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Modifier.tutorialTarget(
    stepId: String,
    viewModel: TutorialViewModel?
): Modifier = if (viewModel == null) this else this.composed {
    val activeStep by viewModel.activeStep.collectAsState()
    val activeStepId = activeStep?.id
    val requester = remember { BringIntoViewRequester() }
    this.bringIntoViewRequester(requester)
        .onGloballyPositioned { coords -> if (stepId == activeStepId && coords.isAttached) { val bounds = coords.boundsInWindow(); if (bounds.width > 0f && bounds.height > 0f) { viewModel.updateTargetBounds(bounds) } } }
        .then(Modifier.composed { LaunchedEffect(activeStepId) { if (stepId == activeStepId) { requester.bringIntoView() } }; Modifier })
}
