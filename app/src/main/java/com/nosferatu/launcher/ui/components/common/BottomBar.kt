package com.nosferatu.launcher.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nosferatu.launcher.ui.LocalAppColors
import com.nosferatu.launcher.ui.ScreenSelectionTab

@Composable
fun BottomBar(
    selectedTab: ScreenSelectionTab,
    onTabSelected: (ScreenSelectionTab) -> Unit
) {
    val colors = LocalAppColors.current
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.onBg.copy(alpha = 0.2f)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScreenSelectionTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(tab) }
                ) {
                    Icon(
                        painter = painterResource(id = tab.iconRes),
                        contentDescription = stringResource(id = tab.labelRes),
                        modifier = Modifier.size(22.dp),
                        tint = colors.onBg
                    )
                    Text(
                        text = stringResource(id = tab.labelRes),
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = colors.onBg
                    )
                    if (isSelected) {
                        Box(modifier = Modifier.padding(top = 2.dp).size(4.dp).background(colors.onBg))
                    }
                }
            }
        }
    }
}