package com.blackstack.beatbox.ui.dialogs

import com.blackstack.beatbox.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Ekolayzer ön ayar seçimi. */
@Composable
fun EQDialog(
    strings: Strings,
    currentEQPreset: EQPreset,
    visualizerWaveform: FloatArray,
    onPresetSelected: (EQPreset) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = { 
            Button(
                { onDismiss() }, 
                modifier = Modifier.buttonShadow(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) { 
                Text(strings.get("ok"), fontWeight = FontWeight.SemiBold) 
            } 
        },
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Equalizer, 
                        null, 
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(strings.get("equalizer"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        strings.get("eq_subtitle"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Live spectrum visualizer preview inside EQ dialog
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val waveform = visualizerWaveform
                        val barCount = if (waveform.isNotEmpty()) minOf(12, waveform.size) else 12
                        for (i in 0 until barCount) {
                            val heightFraction = if (waveform.isNotEmpty() && i < waveform.size) waveform[i].coerceIn(0.1f, 1f) else 0.2f
                            Box(
                                Modifier
                                    .width(4.dp)
                                    .fillMaxHeight(heightFraction)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                EQPreset.entries.forEach { p ->
                    val isSelected = currentEQPreset == p
                    val presetIcon = when(p) {
                        EQPreset.NONE -> Icons.Rounded.MusicNote
                        EQPreset.BASS_BOOST -> Icons.Rounded.GraphicEq
                        EQPreset.VOCAL -> Icons.Rounded.Mic
                        EQPreset.TREBLE -> Icons.AutoMirrored.Rounded.TrendingUp
                        EQPreset.LOUDNESS -> Icons.AutoMirrored.Rounded.VolumeUp
                    }
                    val presetColor = when(p) {
                        EQPreset.NONE -> Color(0xFF9E9E9E)
                        EQPreset.BASS_BOOST -> Color(0xFFFF5722)
                        EQPreset.VOCAL -> Color(0xFF2196F3)
                        EQPreset.TREBLE -> Color(0xFF4CAF50)
                        EQPreset.LOUDNESS -> Color(0xFFFF9800)
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPresetSelected(p) }
                            .cardShadow(),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) 
                            presetColor.copy(alpha = 0.15f) 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        border = if (isSelected) 
                            BorderStroke(2.dp, presetColor) 
                        else null
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(42.dp)
                                    .background(
                                        if (isSelected) presetColor.copy(alpha = 0.25f) else Color.Transparent,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    presetIcon,
                                    null,
                                    tint = if (isSelected) presetColor else MaterialTheme.colorScheme.onSurface.copy(0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    strings.getEQPresetLabel(p),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 15.sp,
                                    color = if (isSelected) presetColor else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    strings.getEQPresetDesc(p),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    null,
                                    tint = presetColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

/** Uyku zamanlayıcısı. */
@Composable
fun SleepTimerDialog(
    strings: Strings,
    onStart: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    var m by remember { mutableIntStateOf(15) }
    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            Button(
                onClick = { 
                    onStart(m)
                    onDismiss() 
                },
                modifier = Modifier
                    .buttonShadow()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        ),
                        RoundedCornerShape(12.dp)
                    ),
                colors = ButtonDefaults.buttonColors(Color.Transparent)
            ) {
                Icon(Icons.Rounded.Nightlight, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.get("start"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { 
            TextButton(
                onClick = { onDismiss() },
                modifier = Modifier.buttonShadow()
            ) { 
                Text(strings.get("cancel")) 
            } 
        },
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Nightlight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text(strings.get("sleep_timer"), fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Beautiful time display with seconds
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format(locale, "%02d:00", m),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${strings.get("minutes")}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Slider with custom styling
                Column {
                    Slider(
                        value = m.toFloat(),
                        onValueChange = { m = it.toInt() },
                        valueRange = 5f..120f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("5 min", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        Text("120 min", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    }
                }
            }
        }
    )
}
