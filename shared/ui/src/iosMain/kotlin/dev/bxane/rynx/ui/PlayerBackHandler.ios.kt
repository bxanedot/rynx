package dev.bxane.rynx.ui

import androidx.compose.runtime.Composable

/** iOS navigation dismissal is owned by the embedding host. */
@Composable
internal actual fun PlayerBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
