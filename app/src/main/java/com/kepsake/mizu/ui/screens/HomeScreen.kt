package com.kepsake.mizu.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(innerPadding: PaddingValues) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()


    fun goToPage(page: Int) {
        coroutineScope.launch {
            pagerState.scrollToPage(page)
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = innerPadding.calculateBottomPadding())
    ) {
        HorizontalPager(
            userScrollEnabled = false,
            state = pagerState,
            modifier = Modifier.weight(1f),
            beyondViewportPageCount = 10
        ) { page ->
            when (page) {
                0 -> LibraryTab(innerPadding)
                1 -> RecentsTab(innerPadding)
            }
        }
        NavigationBar(
            windowInsets = WindowInsets(top = 0, bottom = 0),
            modifier = Modifier.height(60.dp)

        ) {
            NavigationBarItem(
                selected = pagerState.currentPage == 0,
                icon = { Icon(Icons.Rounded.Settings, contentDescription = "Library") },
                onClick = { goToPage(0) },
            )
            NavigationBarItem(
                selected = pagerState.currentPage == 1,
                icon = { Icon(Icons.Rounded.FavoriteBorder, contentDescription = "Recents") },
                onClick = { goToPage(1) },
            )
        }
    }
}

