package com.kepsake.mizu.ui.components

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
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

/**
 * A LazyColumn wrapper that provides a callback when the visible page changes.
 *
 * @param modifier Modifier to be applied to the LazyColumn
 * @param state Optional LazyListState for controlling the list externally
 * @param contentPadding PaddingValues to be applied to the LazyColumn
 * @param reverseLayout Whether the LazyColumn should be in reverse layout
 * @param verticalArrangement Vertical arrangement of the LazyColumn
 * @param horizontalAlignment Horizontal alignment of the LazyColumn
 * @param flingBehavior Fling behavior of the LazyColumn
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
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    onPageChange: (Int) -> Unit,
    content: LazyListScope.() -> Unit
) {
    // Track the current page to avoid unnecessary callback invocations
    var currentPage by remember { mutableStateOf(-1) }

    // Use snapshotFlow to react to scroll state changes
    LaunchedEffect(state) {
        snapshotFlow {
            val layoutInfo = state.layoutInfo

            // First check for fully visible items
            val fullyVisibleItems = layoutInfo.visibleItemsInfo.filter { itemInfo ->
                itemInfo.offset >= layoutInfo.viewportStartOffset &&
                        itemInfo.offset + itemInfo.size <= layoutInfo.viewportEndOffset
            }

            // If there are fully visible items, return the last one
            if (fullyVisibleItems.isNotEmpty()) {
                fullyVisibleItems.last().index
            }
            // If no items are fully visible, return the first partially visible item
            else if (layoutInfo.visibleItemsInfo.isNotEmpty()) {
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
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        content = content
    )
}