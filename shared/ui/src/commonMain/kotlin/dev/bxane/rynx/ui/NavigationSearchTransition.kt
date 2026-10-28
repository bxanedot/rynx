package dev.bxane.rynx.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import dev.bxane.rynx.design.RynxNavigationMotionSpec
import dev.bxane.rynx.design.RynxNavigationMotionSpec.SearchDamping
import dev.bxane.rynx.design.RynxNavigationMotionSpec.SearchStiffness
import dev.bxane.rynx.ui.material.RynxSearchInput

/** One search surface grows from a compact capsule; its text field and query keep their identity. */
@Composable
internal fun NavigationSearchTransition(
    expanded: Boolean,
    requestFocusOnExpand: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    onFocusConsumed: () -> Unit,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(expanded, reducedMotion) {
        if (reducedMotion) progress.snapTo(if (expanded) 1f else 0f)
        else progress.animateTo(
            if (expanded) 1f else 0f,
            spring(
                stiffness = SearchStiffness,
                dampingRatio = SearchDamping,
                visibilityThreshold = .001f,
            ),
        )
        if (!expanded) {
            keyboard?.hide()
            focusManager.clearFocus()
        }
    }

    val fieldReadyForFocus = progress.value >= RynxNavigationMotionSpec.SearchFocusThreshold
    LaunchedEffect(expanded, requestFocusOnExpand, fieldReadyForFocus) {
        if (expanded && requestFocusOnExpand && fieldReadyForFocus) {
            focusRequester.requestFocus()
            keyboard?.show()
            onFocusConsumed()
        }
    }

    val visible = expanded || progress.value > .001f
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
            .height(if (visible) 52.dp else 0.dp)
            .clipToBounds(),
    ) {
        if (visible) {
            val compactWidth = minOf(maxWidth, 52.dp)
            val fieldWidth = compactWidth + (maxWidth - compactWidth) * progress.value
            Box(Modifier.width(fieldWidth).height(52.dp).clipToBounds()) {
                RynxSearchInput(
                    value = value,
                    onChange = onValueChange,
                    onSearch = onSearch,
                    modifier = Modifier.fillMaxWidth(),
                    expansionProgress = progress.value,
                    focusRequester = focusRequester,
                )
            }
        }
    }
}
