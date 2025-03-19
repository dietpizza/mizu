package com.kepsake.mizu.ui.components

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import com.kepsake.mizu.ui.animation.CustomFlingBehaviour

@Composable
fun PageTrackingLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    userScrollEnabled: Boolean = true,
    onPageChange: (Int) -> Unit,
    content: LazyListScope.() -> Unit
) {
    var currentPage by remember { mutableStateOf(-1) }
    val snapLayoutIntoProvider = remember(state) {
        SnapLayoutInfoProvider(state, SnapPosition.Center)
    }
    val snapFlingBehavior = rememberSnapFlingBehavior(snapLayoutIntoProvider)

    val combinedFlingBehavior = remember(state) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                val dampenedVelocity = initialVelocity * 0.75f
                return with(snapFlingBehavior) {
                    performFling(dampenedVelocity)
                }
            }
        }
    }

    LaunchedEffect(state) {
        snapshotFlow {
            val layoutInfo = state.layoutInfo

            val fullyVisibleItems = layoutInfo.visibleItemsInfo.filter { itemInfo ->
                itemInfo.offset >= layoutInfo.viewportStartOffset &&
                        itemInfo.offset + itemInfo.size <= layoutInfo.viewportEndOffset
            }

            if (fullyVisibleItems.isNotEmpty()) {
                fullyVisibleItems.last().index
            } else if (layoutInfo.visibleItemsInfo.isNotEmpty()) {
                layoutInfo.visibleItemsInfo.first().index
            } else {
                -1 // No items visible
            }
        }
            .distinctUntilChanged() // Only emit when the value changes
            .collect { newPage ->
                if (newPage >= 0 && newPage != currentPage) {
                    currentPage = newPage
                    onPageChange(newPage)
                }
            }
    }

    LazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
//        flingBehavior = combinedFlingBehavior,
        flingBehavior = CustomFlingBehaviour(
//            flingDecay = splineBasedDecay(Density(15f))
            flingDecay = exponentialDecay(
                frictionMultiplier = 1.25f,
                absVelocityThreshold = 0.1f
            )
        ),
        userScrollEnabled = userScrollEnabled,
        content = content
    )
}



