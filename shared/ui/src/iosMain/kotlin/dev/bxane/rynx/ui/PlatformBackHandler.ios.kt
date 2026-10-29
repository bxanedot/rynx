package dev.bxane.rynx.ui

/** iOS navigation dismissal is owned by the embedding host. */
internal actual fun RynxPlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
