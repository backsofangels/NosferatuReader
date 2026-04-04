package com.nosferatu.launcher.ui.components.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.dimensionResource
import com.nosferatu.launcher.R
import com.nosferatu.launcher.ui.LocalAppColors

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
        color = colors.onBgMuted,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(id = R.dimen.spacing_16),
                end = dimensionResource(id = R.dimen.spacing_16),
                top = dimensionResource(id = R.dimen.spacing_lg),
                bottom = dimensionResource(id = R.dimen.spacing_sm)
            )
    )
}
