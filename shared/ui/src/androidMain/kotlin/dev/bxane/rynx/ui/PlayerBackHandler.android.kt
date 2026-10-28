package dev.bxane.rynx.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable internal actual fun PlayerBackHandler(enabled:Boolean,onBack:()->Unit) { BackHandler(enabled,onBack) }
