package com.kepsake.mizu.ui.components

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

/**
 * A LazyColumn wrapper that provides a callback when the visible page changes.
 *
 * @param modifier Modifier to be applied to the LazyColumn
 * @param state Optional LazyListState for controlling the list externally
 * @param contentPadding PaddingValues to be applied to the LazyColumn
 * @param reverseLayout Whether the LazyColumn should be in reverse layout
 * @param verticalArrangement Vertical arrangement of the LazyColumn
 * @param horizontalAlignment Horizontal alignment of the LazyColumn
 * @param userScrollEnabled Whether user can scroll the LazyColumn
 * @param onPageChange Callback that fires when the current visible page changes
 * @param content LazyListScope content of the LazyColumn
 */
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
                val dampenedVelocity = initialVelocity * 0.5f
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
        flingBehavior = combinedFlingBehavior,
        userScrollEnabled = userScrollEnabled,
        content = content
    )
}

