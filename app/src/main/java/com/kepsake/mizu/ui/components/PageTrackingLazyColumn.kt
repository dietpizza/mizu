package com.kepsake.mizu.ui.components

import android.util.Log

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


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
