package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.PlannerViewMode

import androidx.compose.material.icons.filled.BarChart

@Composable
fun BottomNavigationBar(
    currentViewMode: PlannerViewMode,
    onViewModeSelect: (PlannerViewMode) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .testTag("bottom_nav_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                selected = currentViewMode == PlannerViewMode.DAY,
                onClick = { onViewModeSelect(PlannerViewMode.DAY) },
                icon = Icons.Default.CalendarViewDay,
                label = "Day",
                testTag = "nav_item_day"
            )
            NavItem(
                selected = currentViewMode == PlannerViewMode.TASKS,
                onClick = { onViewModeSelect(PlannerViewMode.TASKS) },
                icon = Icons.Default.CheckCircleOutline,
                label = "Tasks",
                testTag = "nav_item_tasks"
            )
            NavItem(
                selected = currentViewMode == PlannerViewMode.STATS,
                onClick = { onViewModeSelect(PlannerViewMode.STATS) },
                icon = Icons.Default.BarChart,
                label = "Stats",
                testTag = "nav_item_stats"
            )
            NavItem(
                selected = currentViewMode == PlannerViewMode.WALLET,
                onClick = { onViewModeSelect(PlannerViewMode.WALLET) },
                icon = Icons.Default.AccountBalanceWallet,
                label = "Wallet",
                fontSize = 11.sp,
                testTag = "nav_item_wallet"
            )
            NavItem(
                selected = currentViewMode == PlannerViewMode.CLOSET,
                onClick = { onViewModeSelect(PlannerViewMode.CLOSET) },
                icon = Icons.Default.Checkroom,
                label = "Closet",
                fontSize = 11.sp,
                testTag = "nav_item_closet"
            )
        }
    }
}

@Composable
private fun RowScope.NavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    testTag: String,
    fontSize: TextUnit = 12.sp
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val contentColor = if (selected) activeColor else inactiveColor

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(vertical = 4.dp, horizontal = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = fontSize,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

