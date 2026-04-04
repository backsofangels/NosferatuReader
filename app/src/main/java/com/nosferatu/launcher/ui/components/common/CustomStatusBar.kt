package com.nosferatu.launcher.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.dimensionResource
import com.nosferatu.launcher.ui.LocalAppColors

@Composable
fun CustomStatusBar() {
    val colors = LocalAppColors.current

    Column(modifier = Modifier.background(colors.bg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = com.nosferatu.launcher.R.dimen.spacing_16), vertical = dimensionResource(id = com.nosferatu.launcher.R.dimen.spacing_sm)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ClockItem()
            BatteryStatus()
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = dimensionResource(id = com.nosferatu.launcher.R.dimen.divider_thin),
            color = colors.onBg
        )
    }
}