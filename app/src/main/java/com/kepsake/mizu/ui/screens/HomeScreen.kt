package com.kepsake.mizu.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(innerPadding: PaddingValues) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()


    fun goTo(page: Int) {
        coroutineScope.launch {
            pagerState.scrollToPage(page)
        }
    }


    Column(modifier = Modifier.fillMaxSize()) {
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
        NavigationBar {
            NavigationBarItem(
                icon = { Icon(Icons.Rounded.Settings, contentDescription = "Library") },
                label = { Text("Library") },
                selected = pagerState.currentPage == 0,
                onClick = { goTo(0) }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Rounded.FavoriteBorder, contentDescription = "Recents") },
                label = { Text("Recents") },
                selected = pagerState.currentPage == 1,
                onClick = { goTo(1) }
            )
        }
    }
}

