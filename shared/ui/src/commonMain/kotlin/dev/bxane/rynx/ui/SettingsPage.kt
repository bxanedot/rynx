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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.PasswordVisualTransformation
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bxane.rynx.design.RynxAppearance
import dev.bxane.rynx.design.RynxMaterialMode
import dev.bxane.rynx.ui.material.LocalRynxReducedMotion
import dev.bxane.rynx.playback.scrobble.ScrobbleProvider

/** iOS-style grouped settings for capabilities Rynx currently implements. */
@Composable
internal fun SettingsPage(
    state: PlayerUiState,
    actions: PlayerActions,
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
    var listenBrainzToken by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize()
            .background(background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .semantics { paneTitle = "Settings" },
    ) {
        Box(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 16.dp)) {
            BasicText(
                "Settings",
                Modifier.align(Alignment.Center),
                style = TextStyle(color = ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
            )
            val interaction = remember { MutableInteractionSource() }
            Box(
                Modifier.align(Alignment.CenterEnd).size(38.dp).clip(RoundedCornerShape(19.dp))
                    .clickable(role = Role.Button, interactionSource = interaction, indication = null, onClick = onClose)
                    .semantics { contentDescription = "Close settings" },
                contentAlignment = Alignment.Center,
            ) {
                RynxGlyph(Glyph.Close, secondary, Modifier.size(17.dp))
            }
        }

        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(23.dp),
        ) {
            SettingsGroup("AUDIO", card, secondary, divider) {
                SettingsRow(
                    title = "Sound & EQ",
                    ink = ink,
                    secondary = secondary,
                    onClick = onOpenAudio,
                )
                SettingsDivider(divider)
                SettingsRow(
                    title = "Audio Output",
                    value = state.audioOutputName,
                    ink = ink,
                    secondary = secondary,
                    enabled = state.audioOutputAvailable,
                    onClick = onShowAudioOutput,
                )
            }

            SettingsGroup("APPEARANCE", card, secondary, divider) {
                SettingsToggleRow(
                    title = "Dark Appearance",
                    checked = appearance.darkTheme,
                    enabled = appearance.material != RynxMaterialMode.Amoled,
                    ink = ink,
                    onChange = { onAppearanceChange(appearance.copy(darkTheme = it)) },
                )
                SettingsDivider(divider)
                SettingsRow(
                    title = "Interface & Motion",
                    value = appearance.material.label,
                    ink = ink,
                    secondary = secondary,
                    onClick = onOpenAppearance,
                )
            }

            SettingsGroup("LIBRARY", card, secondary, divider) {
                SettingsRow(
                    title = "Import Music",
                    ink = ink,
                    secondary = secondary,
                    onClick = onImportFiles,
                )
                SettingsDivider(divider)
                SettingsRow(
                    title = "Manage Library",
                    ink = ink,
                    secondary = secondary,
                    onClick = onOpenLibrary,
                )
                if (state.playlistBackupAvailable) {
                    SettingsDivider(divider)
                    SettingsRow(
                        title = "Import Playlist Backup",
                        ink = ink,
                        secondary = secondary,
                        onClick = onImportPlaylistBackup,
                    )
                    if (state.playlists.isNotEmpty()) {
                        SettingsDivider(divider)
                        SettingsRow(
                            title = "Export Playlist Backup",
                            ink = ink,
                            secondary = secondary,
                            onClick = onExportPlaylistBackup,
                        )
                    }
                }
            }

            SettingsGroup("SCROBBLING", card, secondary, divider) {
                val lastFm = state.scrobbling.lastFm
                if (lastFm.connected) {
                    SettingsInfoRow("Last.fm", lastFm.username ?: "Connected", ink, secondary)
                    SettingsDivider(divider)
                    SettingsToggleRow(
                        title = "Scrobble to Last.fm",
                        checked = lastFm.enabled,
                        enabled = true,
                        ink = ink,
                        onChange = { actions.setScrobblingEnabled(ScrobbleProvider.LastFm, it) },
                    )
                    SettingsDivider(divider)
                    SettingsRow("Disconnect Last.fm", ink = ink, secondary = secondary, onClick = actions::disconnectLastFm)
                } else {
                    SettingsRow(
                        title = if (lastFm.pendingApproval) "Finish Last.fm login" else "Connect Last.fm",
                        value = if (state.scrobbling.lastFmConfigured) null else "Setup required",
                        ink = ink,
                        secondary = secondary,
                        enabled = state.scrobbling.lastFmConfigured,
                        onClick = if (lastFm.pendingApproval) actions::finishLastFmLogin else actions::beginLastFmLogin,
                    )
                    if (!state.scrobbling.lastFmConfigured) {
                        BasicText(
                            "Last.fm requires Rynx app credentials configured at build time.",
                            Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp),
                            style = TextStyle(color = secondary, fontSize = 13.sp),
                        )
                    }
                }
                lastFm.error?.let { error ->
                    BasicText(error, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = TextStyle(color = Color(0xFFFF453A), fontSize = 13.sp))
                }

                SettingsDivider(divider)
                val listenBrainz = state.scrobbling.listenBrainz
                if (listenBrainz.connected) {
                    SettingsInfoRow("ListenBrainz", listenBrainz.username ?: "Connected", ink, secondary)
                    SettingsDivider(divider)
                    SettingsToggleRow(
                        title = "Scrobble to ListenBrainz",
                        checked = listenBrainz.enabled,
                        enabled = true,
                        ink = ink,
                        onChange = { actions.setScrobblingEnabled(ScrobbleProvider.ListenBrainz, it) },
                    )
                    SettingsDivider(divider)
                    SettingsRow("Disconnect ListenBrainz", ink = ink, secondary = secondary, onClick = actions::disconnectListenBrainz)
                } else {
                    SettingsRow(
                        title = if (listenBrainz.pendingApproval) "Open ListenBrainz settings" else "Connect ListenBrainz",
                        ink = ink,
                        secondary = secondary,
                        onClick = actions::beginListenBrainzLogin,
                    )
                    if (listenBrainz.pendingApproval) {
                        BasicText(
                            "Create a personal token in ListenBrainz settings, then paste it here.",
                            Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
                            style = TextStyle(color = secondary, fontSize = 13.sp),
                        )
                        BasicTextField(
                            value = listenBrainzToken,
                            onValueChange = { listenBrainzToken = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            singleLine = true,
                            textStyle = TextStyle(color = ink, fontSize = 15.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = PasswordVisualTransformation(),
                            decorationBox = { inner ->
                                Box {
                                    if (listenBrainzToken.isEmpty()) BasicText("Personal token", style = TextStyle(color = secondary, fontSize = 15.sp))
                                    inner()
                                }
                            },
                        )
                        SettingsRow(
                            title = "Verify and connect",
                            ink = ink,
                            secondary = secondary,
                            enabled = listenBrainzToken.isNotBlank(),
                            onClick = {
                                val token = listenBrainzToken.trim()
                                listenBrainzToken = ""
                                actions.connectListenBrainz(token)
                            },
                        )
                    }
                }
                listenBrainz.error?.let { error ->
                    BasicText(error, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = TextStyle(color = Color(0xFFFF453A), fontSize = 13.sp))
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
    title: String,
    value: String? = null,
    ink: Color,
    secondary: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 54.dp)
            .clickable(enabled = enabled, role = Role.Button, interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { contentDescription = title; if (!enabled) disabled() }
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(title, Modifier.weight(1f), style = TextStyle(color = ink.copy(alpha = if (enabled) 1f else .44f), fontSize = 16.sp))
        value?.let {
            BasicText(it, Modifier.padding(start = 8.dp), style = TextStyle(color = secondary.copy(alpha = if (enabled) 1f else .5f), fontSize = 15.sp))
        }
        RynxGlyph(Glyph.ChevronRight, secondary.copy(alpha = if (enabled) .72f else .3f), Modifier.size(15.dp))
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    enabled: Boolean,
    ink: Color,
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
        Modifier.fillMaxWidth().heightIn(min = 54.dp)
            .clickable(enabled = enabled, role = Role.Switch, interactionSource = interaction, indication = null) { onChange(!checked) }
            .semantics {
                contentDescription = title
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                if (!enabled) disabled()
            }
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(title, Modifier.weight(1f), style = TextStyle(color = ink, fontSize = 16.sp))
        Box(Modifier.size(49.dp, 29.dp).clip(RoundedCornerShape(15.dp)).background(if (checked) Color(0xFF34C759) else Color(0xFF787880))) {
            Box(Modifier.padding(2.dp).offset(x = (20f * fraction).dp).size(25.dp).clip(RoundedCornerShape(13.dp)).background(Color.White))
        }
    }
}

@Composable
private fun SettingsDivider(color: Color) {
    Spacer(Modifier.fillMaxWidth().padding(start = 16.dp).height(.5.dp).background(color))
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
