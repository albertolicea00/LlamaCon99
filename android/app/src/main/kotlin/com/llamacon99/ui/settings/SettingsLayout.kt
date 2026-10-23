package com.llamacon99.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.llamacon99.R
import com.llamacon99.ui.contacts.DotMatrixHeading
import com.llamacon99.ui.theme.LocalAppTheme
import com.llamacon99.ui.theme.ThemeStyle
import kotlinx.coroutines.launch

/** Each profile owns its page hierarchy; preference state and actions remain shared. */
@Composable
internal fun SettingsLayout(onBack: () -> Unit, overlays: @Composable () -> Unit = {}, content: LazyListScope.() -> Unit) {
    val theme = LocalAppTheme.current
    val style = theme.style
    val glass = style == ThemeStyle.APPLE
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backdropLayer = rememberGraphicsLayer()
    val scrollingLayer = rememberGraphicsLayer()
    var origin by remember { mutableStateOf(Offset.Zero) }
    var listOrigin by remember { mutableStateOf(Offset.Zero) }
    val state = rememberLazyListState()
    val collapsed by remember { derivedStateOf { state.firstVisibleItemIndex > 0 } }
    val scope = rememberCoroutineScope()
    val background = MaterialTheme.colorScheme.background
    BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
        val wideFluent = style == ThemeStyle.FLUENT && maxWidth >= 720.dp
        Canvas(Modifier.matchParentSize().drawWithContent {
            backdropLayer.record { this@drawWithContent.drawContent() }
            drawLayer(backdropLayer)
        }) {
            drawRect(if (glass) { if (dark) Color(0xFF111114) else Color(0xFFE5E5EA) } else background)
            if (glass || style == ThemeStyle.FLUENT) {
                drawRect(Brush.linearGradient(listOf(
                    if (dark) Color(0xFF2C2C32) else Color(0xFFF9F9FD),
                    if (dark) Color(0xFF141416) else Color(0xFFDADAE2),
                    if (dark) Color(0xFF242428) else Color(0xFFF0F0F5),
                )))
                drawCircle(if (dark) Color.White.copy(alpha = 0.025f) else Color.White.copy(alpha = 0.12f),
                    radius = size.width * 0.62f, center = Offset(size.width * 0.08f, size.height * 0.35f))
            }
        }
        CompositionLocalProvider(LocalSettingsGlass provides SettingsGlassBackdrop(backdropLayer, origin)) {
            Row(Modifier.fillMaxSize()) {
                if (wideFluent) {
                    Column(Modifier.width(208.dp).fillMaxHeight().statusBarsPadding().padding(16.dp)) {
                        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 24.dp))
                        listOf(R.string.settings_section_preferences to 1, R.string.settings_section_caller_id to 3,
                            R.string.settings_section_about to 6).forEach { (label, index) ->
                            TextButton(onClick = { scope.launch { state.animateScrollToItem(index) } },
                                modifier = Modifier.fillMaxWidth()) { Text(stringResource(label)) }
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                            Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.contacts_list_title))
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    androidx.compose.foundation.lazy.LazyColumn(
                        state = state,
                        modifier = Modifier.fillMaxSize()
                            .then(if (glass) Modifier else Modifier.padding(top = statusInset + 56.dp))
                            .clipToBounds().onGloballyPositioned { listOrigin = it.positionInRoot() }
                            .drawWithContent {
                                if (glass) {
                                    scrollingLayer.record {
                                        // Capture an opaque scene so sharp text cannot show through
                                        // transparent pixels of the blurred navigation backdrop.
                                        drawContext.canvas.save()
                                        drawContext.canvas.translate(origin.x - listOrigin.x, origin.y - listOrigin.y)
                                        drawLayer(backdropLayer)
                                        drawContext.canvas.restore()
                                        this@drawWithContent.drawContent()
                                    }
                                    drawLayer(scrollingLayer)
                                } else drawContent()
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(start = theme.settingsInset, end = theme.settingsInset,
                            top = if (glass) statusInset + 72.dp else 8.dp,
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp),
                    ) {
                        item(key = "settings_heading") {
                            val headingModifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()
                                .padding(top = if (style == ThemeStyle.BRAND) 16.dp else 8.dp, bottom = 20.dp)
                            if (style == ThemeStyle.MONO) DotMatrixHeading(stringResource(R.string.settings_title), headingModifier)
                            else Text(stringResource(R.string.settings_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontSize = when (style) { ThemeStyle.BRAND -> 38.sp; ThemeStyle.APPLE -> 34.sp; else -> 30.sp },
                                fontWeight = if (style == ThemeStyle.MATERIAL) FontWeight.Normal else FontWeight.Bold,
                                modifier = headingModifier.semantics { heading() })
                        }
                        content()
                    }
                    if (glass) {
                        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(statusInset + 24.dp)
                            .background(Brush.verticalGradient(listOf(background.copy(alpha = 0.98f), background.copy(alpha = 0.65f), Color.Transparent))))
                        SettingsGlass(shape = RoundedCornerShape(28.dp),
                            backdrop = SettingsGlassBackdrop(scrollingLayer, listOrigin),
                            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Row(Modifier.heightIn(min = 52.dp).padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.settings_back),
                                        tint = MaterialTheme.colorScheme.primary)
                                }
                                Text(stringResource(if (collapsed) R.string.settings_title else R.string.contacts_list_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.clickable(enabled = !collapsed, onClick = onBack).padding(end = 20.dp))
                            }
                        }
                    } else {
                        Row(Modifier.align(Alignment.TopStart).fillMaxWidth().statusBarsPadding()
                            .background(background).padding(horizontal = 8.dp).heightIn(min = 56.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_back))
                            }
                            AnimatedVisibility(collapsed) {
                                Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }
            overlays()
        }
    }
}
