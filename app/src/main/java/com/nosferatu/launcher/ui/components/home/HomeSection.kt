package com.nosferatu.launcher.ui.components.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.dimensionResource
import com.nosferatu.launcher.ui.LocalAppColors

@Composable
fun HomeSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = dimensionResource(id = com.nosferatu.launcher.R.dimen.spacing_lg))
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = LocalAppColors.current.onBg,
            modifier = Modifier
                .padding(start = dimensionResource(id = com.nosferatu.launcher.R.dimen.spacing_16), end = dimensionResource(id = com.nosferatu.launcher.R.dimen.spacing_16), bottom = dimensionResource(id = com.nosferatu.launcher.R.dimen.spacing_sm))
        )
        content()
    }
}