package com.octarahq.trainflow.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.core.screen.Screen
import com.octarahq.trainflow.ui.screens.*
import kotlinx.coroutines.launch

private val DrawerPanel = Color(0xFF1D2026)
private val DrawerBorder = Color(0xFF2A303C)
private val DrawerTextPrimary = Color(0xFFF1F5F9)
private val DrawerTextSecondary = Color(0xFF94A3B8)

@Composable
fun TrainflowDrawer(
    navigator: Navigator,
    currentScreen: Screen?,
    drawerState: DrawerState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val rawVersion = com.octarahq.trainflow.ui.utils.getAppVersion()
    val versionDisplay = rawVersion.trim().removePrefix("v").removePrefix("V")

    ModalDrawerSheet(
        drawerContainerColor = DrawerPanel,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Column {
                Text(
                    text = "Trainflow",
                    style = MaterialTheme.typography.titleLarge,
                    color = DrawerTextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                HorizontalDivider(color = DrawerBorder)
                Spacer(modifier = Modifier.height(16.dp))

                DrawerItem(
                    label = "Accueil",
                    isSelected = currentScreen is HomeScreen,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (currentScreen !is HomeScreen) navigator.push(HomeScreen())
                    }
                )
                DrawerItem(
                    label = "Recherche",
                    isSelected = currentScreen is SearchScreen,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (currentScreen !is SearchScreen) navigator.push(SearchScreen())
                    }
                )
                DrawerItem(
                    label = "Alertes",
                    isSelected = currentScreen is AlertsScreen,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (currentScreen !is AlertsScreen) navigator.push(AlertsScreen())
                    }
                )
                DrawerItem(
                    label = "Mes Trajets",
                    isSelected = currentScreen is TripsScreen,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (currentScreen !is TripsScreen) navigator.push(TripsScreen())
                    }
                )
            }

            Text(
                text = "Version : $versionDisplay",
                style = MaterialTheme.typography.bodySmall,
                color = DrawerTextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
        }
    }
}

@Composable
private fun DrawerItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = if (isSelected) DrawerTextPrimary else DrawerTextSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    )
}
