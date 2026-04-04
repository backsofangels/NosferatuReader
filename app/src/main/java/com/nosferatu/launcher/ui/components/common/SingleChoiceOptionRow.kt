package com.nosferatu.launcher.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import com.nosferatu.launcher.R
import com.nosferatu.launcher.ui.LocalAppColors

@Composable
fun SingleChoiceOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dimensionResource(id = R.dimen.row_min_height))
            .background(if (selected) colors.selectedRowBackground else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(role = Role.Button) { onClick() }
            .semantics { this.selected = selected }
            .padding(horizontal = dimensionResource(id = R.dimen.spacing_16), vertical = dimensionResource(id = R.dimen.spacing_sm)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // leading indicator (8dp dot) or placeholder
        val dotSize = dimensionResource(id = R.dimen.spacing_sm)
        if (selected) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(colors.accent)
            )
        } else {
            Spacer(modifier = Modifier.size(dotSize))
        }

        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.spacing_md)))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) colors.onBg else colors.onBgMuted
        )
    }
}
