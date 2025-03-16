package com.kepsake.mizu.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ColorFilter
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.IIcon

@Composable
fun BottomNavigationIcon(selected: Boolean, selectedIcon: IIcon, icon: IIcon) {
    val color =
        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    val displayIcon = if (selected) selectedIcon else icon

    Image(asset = displayIcon, colorFilter = ColorFilter.tint(color))
}
