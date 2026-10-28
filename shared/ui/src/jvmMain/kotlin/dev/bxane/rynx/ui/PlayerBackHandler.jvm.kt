package dev.bxane.rynx.ui

import androidx.compose.runtime.Composable

// The Android back stack is exercised by its native host; this target is also the headless UI test harness.
@Composable internal actual fun PlayerBackHandler(enabled:Boolean,onBack:()->Unit) = Unit
