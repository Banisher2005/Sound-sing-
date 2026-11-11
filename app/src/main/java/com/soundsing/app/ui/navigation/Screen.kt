package com.soundsing.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : Screen("home", "Home", Icons.Default.GraphicEq)
    object Devices : Screen("devices", "Outputs", Icons.Default.Headphones)
    object Compatibility : Screen("compatibility", "Check", Icons.Default.VerifiedUser)
    object Guide : Screen("guide", "Guide", Icons.Default.HelpOutline)

    companion object {
        val bottomNavItems = listOf(Home, Devices, Compatibility, Guide)
    }
}
