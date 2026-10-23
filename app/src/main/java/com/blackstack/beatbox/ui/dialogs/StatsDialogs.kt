package com.blackstack.beatbox.ui.dialogs

import com.blackstack.beatbox.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit

/** Yıl sonu dinleme özeti. */
@Composable
fun WrappedDialog(
    strings: Strings,
    ctx: Context,
    prefs: SharedPreferences,
    allAudioList: List<AudioFile>,
    onDismiss: () -> Unit
) {
    val totalMinutes = prefs.getLong("stats_total_minutes", 0L)
    val topSongs = allAudioList.sortedByDescending { prefs.getInt("stats_count_${it.id}", 0) }.take(5)
    val topSong = topSongs.firstOrNull()
    val topArtist = topSongs.groupBy { it.artist }.maxByOrNull { it.value.size }?.key ?: (topSong?.artist ?: "Bilinmiyor")

    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            Button(
                onClick = {
                    val shareText = "ğŸ”¥ My BeatBox Wrapped ğŸµ\n\nâ±ï¸ Total Listening: $totalMinutes minutes\nğŸ† Top Song: \"${topSong?.title ?: "N/A"}\" by ${topSong?.artist ?: "N/A"}\nğŸ¤ Top Artist: $topArtist\n\nListen on BeatBox!"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "BeatBox Wrapped")
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    ctx.startActivity(Intent.createChooser(intent, "MÃ¼zik Ã–zetini PaylaÅŸ"))
                },
                modifier = Modifier.buttonShadow()
            ) {
                Icon(Icons.Rounded.Share, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ã–zeti PaylaÅŸ", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(strings.get("cancel"))
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Analytics, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("BeatBox Wrapped ğŸµ", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Timer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(strings.get("total_listen"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                            Text("$totalMinutes ${strings.get("minutes")}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                if (topSong != null) {
                    Column {
                        Text(strings.get("most_played_song"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Text("\"${topSong.title}\" - ${topSong.artist}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("${prefs.getInt("stats_count_${topSong.id}", 0)}x", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                if (topSongs.isNotEmpty()) {
                    Column {
                        Text(strings.get("top_5_songs"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        topSongs.forEachIndexed { index, song ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${index + 1}. ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                Text(song.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), fontSize = 13.sp)
                                Text("${prefs.getInt("stats_count_${song.id}", 0)}x", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    )
}

/** Son dinlenen şarkılar. */
@Composable
fun HistoryDialog(
    strings: Strings,
    prefs: SharedPreferences,
    allAudioList: List<AudioFile>,
    onSongClick: (AudioFile, List<AudioFile>) -> Unit,
    onDismiss: () -> Unit
) {
    val historyRaw = prefs.getString("stats_history", "") ?: ""
    val historyIds = historyRaw.split(",").filter { it.isNotBlank() }
    val historySongs = historyIds.mapNotNull { idStr -> allAudioList.find { it.id.toString() == idStr } }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            if (historySongs.isNotEmpty()) {
                TextButton(onClick = {
                    prefs.edit { remove("stats_history") }
                    onDismiss()
                }) {
                    Text("GeÃ§miÅŸi Temizle", color = Color.Red)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(strings.get("cancel"))
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text("Dinleme GeÃ§miÅŸi", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            if (historySongs.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text("HenÃ¼z dinleme geÃ§miÅŸi yok", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 350.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(historySongs, key = { it.id }) { song ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onSongClick(song, historySongs)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)
                        ) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                AlbumArtImage(song, Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(song.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                                    Text(song.artist, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                                }
                                Icon(Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    )
}
