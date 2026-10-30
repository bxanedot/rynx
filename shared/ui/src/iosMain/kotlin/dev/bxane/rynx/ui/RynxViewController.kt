package dev.bxane.rynx.ui

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun RynxViewController(state: PlayerUiState, actions: PlayerActions): UIViewController =
    ComposeUIViewController { RynxApp(state, actions) }
