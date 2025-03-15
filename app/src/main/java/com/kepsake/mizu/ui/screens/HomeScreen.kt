package com.kepsake.mizu.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@Composable
fun HomeScreen(innerPadding: PaddingValues) {
    val pagerState = rememberPagerState(pageCount = { 3 })

    HorizontalPager(
        state = pagerState, modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 10
    ) { page ->
        when (page) {
            0 -> LibraryView(innerPadding)
            1 -> LibraryView(innerPadding)
            2 -> LibraryView(innerPadding)
        }
    }
}