package com.kepsake.mizu.layout

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FixedHeightLinearLayoutManager(
    context: Context
) : LinearLayoutManager(context) {

    private var itemHeights: List<Int> = emptyList()

    fun setItemHeights(heights: List<Int>) {
        this.itemHeights = heights
        requestLayout()
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child != null) {
                val position = getPosition(child)
                if (position < itemHeights.size) {
                    val layoutParams = child.layoutParams as RecyclerView.LayoutParams
                    layoutParams.height = itemHeights[position]
                    child.layoutParams = layoutParams
                }
            }
        }
        super.onLayoutChildren(recycler, state)
    }
}