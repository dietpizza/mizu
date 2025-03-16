package com.kepsake.mizu.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun RecentsTab(innerPadding: PaddingValues) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    Box(
        modifier = Modifier
            .fillMaxSize()
            .width(screenWidth),
        contentAlignment = Alignment.Center
    ) {
        Text("Recents Tab", style = MaterialTheme.typography.titleLarge)
    }
}