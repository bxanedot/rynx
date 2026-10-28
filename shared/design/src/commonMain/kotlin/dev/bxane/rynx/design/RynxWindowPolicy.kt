package dev.bxane.rynx.design

/**
 * Responsive layout capability, not a device identity detector.
 *
 * TV and Wear require explicit platform entry points and input handling;
 * display size alone must never be used to identify those devices.
 */
enum class RynxWindowClass { Compact, Medium, Expanded }

data class RynxWindowPolicy(
    val windowClass: RynxWindowClass,
    val sideNavigation: Boolean,
    val contentMargin: Float,
    val minimumTouchTarget: Float,
)

fun rynxWindowPolicy(widthDp: Float, heightDp: Float): RynxWindowPolicy {
    val width = if (widthDp.isFinite()) widthDp.coerceAtLeast(0f) else 0f
    val height = if (heightDp.isFinite()) heightDp.coerceAtLeast(0f) else 0f
    val category = when {
        width >= 840f -> RynxWindowClass.Expanded
        width >= 600f -> RynxWindowClass.Medium
        else -> RynxWindowClass.Compact
    }
    // Keep the bottom navigation on short landscape windows, where a rail
    // would crowd the player and other primary controls.
    return RynxWindowPolicy(
        windowClass = category,
        sideNavigation = width >= 760f && height >= 600f,
        contentMargin = when(category) {
            RynxWindowClass.Compact -> 20f
            RynxWindowClass.Medium -> 28f
            RynxWindowClass.Expanded -> 36f
        },
        minimumTouchTarget = 48f,
    )
}
