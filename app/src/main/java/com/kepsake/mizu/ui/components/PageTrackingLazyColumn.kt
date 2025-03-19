package com.kepsake.mizu.ui.components

import android.util.Log
import androidx.compose.animation.core.exponentialDecay
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
import com.kepsake.mizu.ui.animation.CustomFlingBehaviour

import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


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

@Composable
fun PageTrackingRecyclerView(
    modifier: Modifier = Modifier,
    scrollToIndex: Int? = null, // New parameter for scrolling to a specific index
    onPageChange: (Int) -> Unit,
    content: RecyclerView.Adapter<RecyclerView.ViewHolder>
) {
    var currentPage by remember { mutableStateOf(-1) }

    AndroidView(
        factory = { context ->
            RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context)
                adapter = content
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                        val firstVisibleItemPosition =
                            layoutManager.findFirstCompletelyVisibleItemPosition()
                        if (firstVisibleItemPosition >= 0 && firstVisibleItemPosition != currentPage) {
                            currentPage = firstVisibleItemPosition
                            onPageChange(firstVisibleItemPosition)
                        }
                    }
                })
            }
        },
        modifier = modifier,
        update = { recyclerView ->
            // Scroll to the specified index when scrollToIndex changes
            Log.e(TAG, "Scrolling to index")
            scrollToIndex?.let { index ->
                if (index >= 0 && index < (recyclerView.adapter?.itemCount ?: 0)) {
                    recyclerView.layoutManager?.scrollToPosition(index)
                }
            }
        }
    )
}


//@Composable
//fun PageTrackingRecyclerView(
//    modifier: Modifier = Modifier,
//    onPageChange: (Int) -> Unit,
//    content: RecyclerView.Adapter<RecyclerView.ViewHolder>
//) {
//    var currentPage by remember { mutableStateOf(-1) }
//
//    AndroidView(
//        factory = { context ->
//            RecyclerView(context).apply {
//                layoutManager = LinearLayoutManager(context)
//                adapter = content
//                addOnScrollListener(object : RecyclerView.OnScrollListener() {
//                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                        super.onScrolled(recyclerView, dx, dy)
//                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
//                        val firstVisibleItemPosition =
//                            layoutManager.findFirstCompletelyVisibleItemPosition()
//                        if (firstVisibleItemPosition >= 0 && firstVisibleItemPosition != currentPage) {
//                            currentPage = firstVisibleItemPosition
//                            onPageChange(firstVisibleItemPosition)
//                        }
//                    }
//                })
//            }
//        },
//        modifier = modifier
//    )
//}
