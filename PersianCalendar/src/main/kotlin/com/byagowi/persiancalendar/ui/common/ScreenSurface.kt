package com.byagowi.persiancalendar.ui.common

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import com.byagowi.persiancalendar.SHARED_CONTENT_KEY_CARD
import com.byagowi.persiancalendar.SHARED_CONTENT_KEY_CARD_CONTENT
import com.byagowi.persiancalendar.ui.theme.animatedSurfaceColor
import com.byagowi.persiancalendar.ui.utils.isOnCI
import com.byagowi.persiancalendar.ui.utils.materialCornerExtraLargeTop

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.ScreenSurface(
    animatedContentScope: AnimatedContentScope,
    shape: Shape = materialCornerExtraLargeTop(),
    content: @Composable () -> Unit,
) {
    Layout(content = {
        Surface(
            shape = shape,
            color = animatedSurfaceColor(),
            // Workaround CI not liking shared elements
            modifier = if (LocalContext.current.isOnCI()) Modifier else Modifier.sharedElement(
                rememberSharedContentState(SHARED_CONTENT_KEY_CARD),
                animatedVisibilityScope = animatedContentScope,
            ),
        ) {}
        Surface(
            if (LocalContext.current.isOnCI()) Modifier else Modifier.sharedBounds(
                rememberSharedContentState(SHARED_CONTENT_KEY_CARD_CONTENT),
                animatedVisibilityScope = animatedContentScope,
            ),
            shape = shape,
            contentColor = animateColorAsState(
                contentColorFor(MaterialTheme.colorScheme.surface),
                label = "content color",
            ).value,
            color = Color.Transparent,
            content = content,
        )
    }) { measurables, constraints ->
        val child = measurables[1].measure(constraints)
        val parent = measurables[0].measure(Constraints.fixed(child.width, child.height))
        layout(child.width, child.height) {
            parent.placeRelative(0, 0)
            child.placeRelative(0, 0)
        }
    }
}
