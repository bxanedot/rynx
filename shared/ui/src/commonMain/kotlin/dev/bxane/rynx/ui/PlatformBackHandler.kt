package dev.bxane.rynx.ui

import androidx.compose.runtime.Composable

@Composable
internal expect fun RynxPlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
