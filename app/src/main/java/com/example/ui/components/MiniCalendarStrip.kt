package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.DateTimeUtils

import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import com.example.ui.theme.CalendarStripBorder
import com.example.ui.theme.CalendarStripSelected

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniCalendarStrip(
    selectedDateMillis: Long,
    onDateSelect: (Long) -> Unit,
    onDateLongClick: (Long) -> Unit = {}
) {
    val weekDays = DateTimeUtils.getWeekDays(selectedDateMillis)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            weekDays.forEach { dayMillis ->
                val isSelected = DateTimeUtils.isSameDay(dayMillis, selectedDateMillis)
                val isToday = DateTimeUtils.isToday(dayMillis)

                val dayName = DateTimeUtils.formatShortDayName(dayMillis)
                val dayNum = DateTimeUtils.formatDayOfMonth(dayMillis)

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .background(if (isSelected) CalendarStripSelected else MaterialTheme.colorScheme.surface)
                        .then(
                            if (isSelected) Modifier.border(width = 1.dp, color = CalendarStripBorder.copy(alpha = 0.4f))
                            else Modifier.border(width = 0.5.dp, color = Color(0xFF2C2C2C))
                        )
                        .combinedClickable(
                            onClick = { onDateSelect(dayMillis) },
                            onLongClick = { onDateLongClick(dayMillis) }
                        )
                        .testTag("day_strip_$dayNum")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = dayName,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF9E9E9E),
                            lineHeight = 10.sp
                        )
                        Text(
                            text = dayNum,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFFE0E0E0),
                            lineHeight = 13.sp
                        )
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .align(Alignment.BottomCenter)
                                .background(CalendarStripBorder)
                        )
                    }
                }
            }
        }
    }
}
