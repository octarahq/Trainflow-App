package com.octarahq.trainflow.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.octarahq.trainflow.ui.navigation.Screen

private val DrawerPanel = Color(0xFF1D2026)
private val DrawerBorder = Color(0xFF2A303C)
private val DrawerTextPrimary = Color(0xFFF1F5F9)
private val DrawerTextSecondary = Color(0xFF94A3B8)

@Composable
fun TrainflowDrawer(
    currentRoute: String,
    onNavigateTo: (String) -> Unit
) {
    val items = listOf(
        Screen.Home,
        Screen.Trips,
        Screen.Search,
        Screen.Alerts
    )

    ModalDrawerSheet(
        drawerContainerColor = DrawerPanel,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Trainflow",
                style = MaterialTheme.typography.titleLarge,
                color = DrawerTextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            HorizontalDivider(color = DrawerBorder)
            Spacer(modifier = Modifier.height(16.dp))
            
            items.forEach { screen ->
                Text(
                    text = screen.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (currentRoute == screen.route) DrawerTextPrimary else DrawerTextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateTo(screen.route) }
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}
