package com.blackstack.beatbox.ui.dialogs

import com.blackstack.beatbox.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Yeni çalma listesi oluşturma. */
@Composable
fun CreatePlaylistDialog(
    strings: Strings,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var n by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            Button(
                onClick = {
                    if (n.isNotBlank()) {
                        onCreate(n.trim())
                        onDismiss()
                    }
                },
                modifier = Modifier.buttonShadow(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(strings.get("ok"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() },
                modifier = Modifier.buttonShadow()
            ) {
                Text(
                    strings.get("cancel"),
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                )
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.QueueMusic,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    strings.get("new_playlist"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = n,
                    onValueChange = { n = it },
                    label = { Text(strings.get("playlist_name")) },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Edit,
                            null,
                            tint = MaterialTheme.colorScheme.primary.copy(0.7f)
                        )
                    },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    strings.get("new_playlist_subtitle"),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                )
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

/** Var olan çalma listesini yeniden adlandırma. */
@Composable
fun EditPlaylistDialog(
    strings: Strings,
    playlist: Playlist,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var n by remember { mutableStateOf(playlist.name) }
    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            Button({
                if (n.isNotBlank()) {
                    onRename(n)
                    onDismiss()
                }
            }, modifier = Modifier.buttonShadow()) { Text(strings.get("ok")) }
        },
        dismissButton = { TextButton({ onDismiss() }, modifier = Modifier.buttonShadow()) { Text(strings.get("cancel")) } },
        title = { Text(strings.get("update")) },
        text = { OutlinedTextField(n, { n = it }, label = { Text(strings.get("playlist_name")) }, shape = RoundedCornerShape(28.dp)) }
    )
}

/** Kullanıcı çalma listesini silmeden önce onay ister. */
@Composable
fun DeletePlaylistDialog(
    strings: Strings,
    playlist: Playlist,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(strings.get("delete"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.buttonShadow()) {
                Text(
                    strings.get("cancel"),
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                )
            }
        },
        title = {
            Text(
                strings.get("delete_playlist_title"),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column {
                Text(
                    playlist.name,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    strings.get("delete_playlist_message"),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

/** Şarkıyı var olan bir çalma listesine ekleme. */
@Composable
fun AddToPlaylistDialog(
    strings: Strings,
    playlists: List<Playlist>,
    song: AudioFile,
    onAdd: (Playlist, AudioFile) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = { TextButton({ onDismiss() }, modifier = Modifier.buttonShadow()) { Text(strings.get("cancel")) } },
        title = { Text(strings.get("add_to_playlist")) },
        text = {
            Column {
                playlists.forEach { p ->
                    val isAdded = p.songIds.contains(song.id)
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            if (!isAdded) onAdd(p, song)
                            onDismiss()
                        }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Text(p.name)
                        Spacer(Modifier.weight(1f))
                        if (isAdded) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    )
}
