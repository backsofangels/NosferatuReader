package com.nosferatu.launcher.ui.components.fontsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nosferatu.launcher.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nosferatu.launcher.library.LibraryConfig
import com.nosferatu.launcher.ui.LocalAppColors
// ...existing imports kept; removed unused ones to clean warnings

@Composable
fun ReaderTextSettings(
    libraryConfig: LibraryConfig,
    onPreferenceChanged: () -> Unit
) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bg)
            .padding(24.dp)
    ) {
        SliderRow(
            label = stringResource(id = R.string.dimension_label),
            currentValue = libraryConfig.fontSizeScale,
            minValue = 0.5f,
            maxValue = 2.5f,
            steps = 20,
            onValueChange = {
                libraryConfig.updateFontSize(it)
                onPreferenceChanged()
            }
        )
    }
}

/**
 * Componente con pulsanti + / - e Slider per controllo granulare.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderRow(
    label: String,
    currentValue: Float,
    minValue: Float,
    maxValue: Float,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            ),
            color = LocalAppColors.current.onBg.copy(alpha = 0.6f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically, // Allinea i centri di pulsanti e slider
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ControlTextButton(stringResource(id = R.string.symbol_minus)) {
                if (currentValue > minValue) {
                    val stepSize = (maxValue - minValue) / (if (steps > 0) steps else 10)
                    onValueChange((currentValue - stepSize).coerceAtLeast(minValue))
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp), // Altezza identica a ControlTextButton
                contentAlignment = Alignment.Center
            ) {
                Slider(
                    value = currentValue,
                    onValueChange = onValueChange,
                    valueRange = minValue..maxValue,
                    steps = steps,
                    modifier = Modifier.fillMaxWidth(),
                    thumb = {
                        Box(
                            Modifier
                                .width(2.dp)
                                .height(20.dp)
                                .background(LocalAppColors.current.onBg)
                        )
                    },
                    track = { sliderState ->
                        // Disegno manuale della linea per il centramento assoluto
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(LocalAppColors.current.progressTrack.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val fraction = (sliderState.value - minValue) / (maxValue - minValue)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .background(LocalAppColors.current.onBg)
                            )
                        }
                    }
                )
            }

            ControlTextButton(stringResource(id = R.string.symbol_plus)) {
                if (currentValue < maxValue) {
                    val stepSize = (maxValue - minValue) / (if (steps > 0) steps else 10)
                    onValueChange((currentValue + stepSize).coerceAtMost(maxValue))
                }
            }
        }
    }
}

@Composable
fun ControlTextButton(
    symbol: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp) // Increased size from 36.dp to 48.dp
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            fontSize = 24.sp, // Increased font size from 20.sp to 24.sp
            fontWeight = FontWeight.Light,
            color = LocalAppColors.current.onBg
        )
    }
}
