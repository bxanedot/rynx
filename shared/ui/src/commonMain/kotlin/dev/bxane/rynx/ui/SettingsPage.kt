package dev.bxane.rynx.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bxane.rynx.design.RynxAppearance
import dev.bxane.rynx.design.RynxMaterialMode
import dev.bxane.rynx.ui.material.LocalRynxReducedMotion
import dev.bxane.rynx.ui.material.RynxAccent

/** iOS-style grouped settings for capabilities Rynx currently implements. */
@Composable
internal fun SettingsPage(
    state: PlayerUiState,
    appearance: RynxAppearance,
    onAppearanceChange: (RynxAppearance) -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenAudio: () -> Unit,
    onShowAudioOutput: () -> Unit,
    onImportFiles: () -> Unit,
    onOpenLibrary: () -> Unit,
    onImportPlaylistBackup: () -> Unit,
    onExportPlaylistBackup: () -> Unit,
    onClose: () -> Unit,
) {
    PlayerBackHandler(true, onClose)
    val dark = appearance.darkTheme || appearance.material == RynxMaterialMode.Amoled
    val background = if (dark) Color.Black else Color(0xFFF2F2F7)
    val card = if (dark) Color(0xFF1C1C1E) else Color.White
    val ink = if (dark) Color.White else Color(0xFF1C1C1E)
    val secondary = if (dark) Color(0xFF98989E) else Color(0xFF6D6D72)
    val divider = if (dark) Color(0xFF38383A) else Color(0xFFE1E1E5)

    Column(
        Modifier.fillMaxSize()
            .background(background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .semantics { paneTitle = "Settings" },
    ) {
        Row(
            Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText("Settings", style = TextStyle(color = ink, fontSize = 34.sp, fontWeight = FontWeight.Bold))
            Spacer(Modifier.weight(1f))
            val interaction = remember { MutableInteractionSource() }
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(21.dp))
                    .clickable(role = Role.Button, interactionSource = interaction, indication = null, onClick = onClose)
                    .semantics { contentDescription = "Close settings" },
                contentAlignment = Alignment.Center,
            ) {
                RynxGlyph(Glyph.Close, secondary, Modifier.size(19.dp))
            }
        }

        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(23.dp),
        ) {
            SettingsGroup("PLAYBACK", card, secondary, divider) {
                SettingsRow(
                    icon = Glyph.Equalizer,
                    iconColor = Color(0xFFFFFFFF),
                    iconSurface = Color(0xFFAF52DE),
                    title = "Sound & EQ",
                    detail = "Equalizer, Pure Mode and sound processing",
                    ink = ink,
                    secondary = secondary,
                    onClick = onOpenAudio,
                )
                SettingsDivider(divider)
                SettingsRow(
                    icon = Glyph.Output,
                    iconColor = Color.White,
                    iconSurface = Color(0xFF007AFF),
                    title = "Audio Output",
                    detail = state.audioOutputName,
                    ink = ink,
                    secondary = secondary,
                    enabled = state.audioOutputAvailable,
                    onClick = onShowAudioOutput,
                )
            }

            SettingsGroup("APPEARANCE", card, secondary, divider) {
                SettingsToggleRow(
                    icon = if (appearance.darkTheme) Glyph.Moon else Glyph.Sun,
                    iconSurface = Color(0xFF5856D6),
                    title = "Dark Appearance",
                    detail = if (appearance.material == RynxMaterialMode.Amoled) "Always on in True AMOLED" else "Use a dark interface",
                    checked = appearance.darkTheme,
                    enabled = appearance.material != RynxMaterialMode.Amoled,
                    ink = ink,
                    secondary = secondary,
                    onChange = { onAppearanceChange(appearance.copy(darkTheme = it)) },
                )
                SettingsDivider(divider)
                SettingsRow(
                    icon = Glyph.Appearance,
                    iconColor = Color.White,
                    iconSurface = Color(0xFFFF2D55),
                    title = "Interface & Motion",
                    detail = "${appearance.material.label}, themes, glass and motion",
                    ink = ink,
                    secondary = secondary,
                    onClick = onOpenAppearance,
                )
            }

            SettingsGroup("LIBRARY", card, secondary, divider) {
                SettingsRow(
                    icon = Glyph.Add,
                    iconColor = Color.White,
                    iconSurface = Color(0xFF34C759),
                    title = "Import Music",
                    detail = "Add audio files from this device",
                    ink = ink,
                    secondary = secondary,
                    onClick = onImportFiles,
                )
                SettingsDivider(divider)
                SettingsRow(
                    icon = Glyph.Library,
                    iconColor = Color.White,
                    iconSurface = Color(0xFFFF9500),
                    title = "Manage Library",
                    detail = "Songs, favorites, history and playlists",
                    ink = ink,
                    secondary = secondary,
                    onClick = onOpenLibrary,
                )
                if (state.playlistBackupAvailable) {
                    SettingsDivider(divider)
                    SettingsRow(
                        icon = Glyph.MoveDown,
                        iconColor = Color.White,
                        iconSurface = Color(0xFF5AC8FA),
                        title = "Import Playlist Backup",
                        detail = "Restore a Rynx playlist archive",
                        ink = ink,
                        secondary = secondary,
                        onClick = onImportPlaylistBackup,
                    )
                    if (state.playlists.isNotEmpty()) {
                        SettingsDivider(divider)
                        SettingsRow(
                            icon = Glyph.MoveUp,
                            iconColor = Color.White,
                            iconSurface = Color(0xFF32ADE6),
                            title = "Export Playlist Backup",
                            detail = "Save playlists as a portable archive",
                            ink = ink,
                            secondary = secondary,
                            onClick = onExportPlaylistBackup,
                        )
                    }
                }
            }

            SettingsGroup("ABOUT", card, secondary, divider) {
                SettingsInfoRow("Application", "rynx", ink, secondary)
                SettingsDivider(divider)
                SettingsInfoRow("Developer", "bxane", ink, secondary)
            }

            BasicText(
                "Settings apply immediately and are saved on this device.",
                Modifier.padding(start = 4.dp, end = 4.dp, bottom = 22.dp),
                style = TextStyle(color = secondary, fontSize = 12.sp),
            )
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    card: Color,
    secondary: Color,
    divider: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(
            title,
            Modifier.padding(start = 5.dp),
            style = TextStyle(color = secondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = .5.sp),
        )
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(card), content = content)
    }
}

@Composable
private fun SettingsRow(
    icon: Glyph,
    iconColor: Color,
    iconSurface: Color,
    title: String,
    detail: String,
    ink: Color,
    secondary: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 66.dp)
            .clickable(enabled = enabled, role = Role.Button, interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { contentDescription = title; if (!enabled) disabled() }
            .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)).background(iconSurface), contentAlignment = Alignment.Center) {
            RynxGlyph(icon, iconColor, Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(title, style = TextStyle(color = ink.copy(alpha = if (enabled) 1f else .44f), fontSize = 15.sp))
            BasicText(detail, style = TextStyle(color = secondary.copy(alpha = if (enabled) 1f else .5f), fontSize = 12.sp))
        }
        RynxGlyph(Glyph.ChevronRight, secondary.copy(alpha = if (enabled) .72f else .3f), Modifier.size(15.dp))
    }
}

@Composable
private fun SettingsToggleRow(
    icon: Glyph,
    iconSurface: Color,
    title: String,
    detail: String,
    checked: Boolean,
    enabled: Boolean,
    ink: Color,
    secondary: Color,
    onChange: (Boolean) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val reduced = LocalRynxReducedMotion.current
    val fraction = animateFloatAsState(
        if (checked) 1f else 0f,
        animationSpec = if (reduced) androidx.compose.animation.core.snap() else spring(),
        label = "Settings dark appearance switch",
    ).value
    Row(
        Modifier.fillMaxWidth().heightIn(min = 66.dp)
            .clickable(enabled = enabled, role = Role.Switch, interactionSource = interaction, indication = null) { onChange(!checked) }
            .semantics {
                contentDescription = title
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                if (!enabled) disabled()
            }
            .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)).background(iconSurface), contentAlignment = Alignment.Center) {
            RynxGlyph(icon, Color.White, Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(title, style = TextStyle(color = ink, fontSize = 15.sp))
            BasicText(detail, style = TextStyle(color = secondary, fontSize = 12.sp))
        }
        Box(Modifier.size(51.dp, 31.dp).clip(RoundedCornerShape(16.dp)).background(if (checked) RynxAccent else Color(0xFF787880))) {
            Box(
                Modifier.padding(2.dp).offset(x = (20f * fraction).dp).size(27.dp)
                    .clip(RoundedCornerShape(14.dp)).background(Color.White),
            )
        }
    }
}

@Composable
private fun SettingsDivider(color: Color) {
    Spacer(Modifier.fillMaxWidth().padding(start = 55.dp).height(.5.dp).background(color))
}

@Composable
private fun SettingsInfoRow(label: String, value: String, ink: Color, secondary: Color) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .semantics { contentDescription = label }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(label, style = TextStyle(color = ink, fontSize = 15.sp))
        Spacer(Modifier.weight(1f))
        BasicText(value, style = TextStyle(color = secondary, fontSize = 14.sp))
    }
}
