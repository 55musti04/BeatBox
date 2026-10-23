package com.blackstack.beatbox.ui.dialogs

import com.blackstack.beatbox.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Müzik taranacak klasörleri seçme. */
@Composable
fun FolderDialog(
    strings: Strings,
    selectedFolders: Set<String>,
    availableFolders: List<String>,
    onToggleFolder: (Set<String>) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            Button(
                onClick = { onConfirm() },
                modifier = Modifier.buttonShadow(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.Check,
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
                        Icons.Rounded.Folder,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        strings.get("folders"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        "${selectedFolders.size} klasÃ¶r seÃ§ili",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary.copy(0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            Column {
                Text(
                    "MÃ¼zik klasÃ¶rlerini seÃ§",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableFolders) { f ->
                        val isSelected = selectedFolders.contains(f)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val n = selectedFolders.toMutableSet()
                                    if (n.contains(f)) n.remove(f) else n.add(f)
                                    onToggleFolder(n)
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer.copy(0.3f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
                            border = if (isSelected)
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(0.5f))
                            else null
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        checkmarkColor = Color.White
                                    )
                                )
                                Spacer(Modifier.width(12.dp))
                                Icon(
                                    Icons.Rounded.Folder,
                                    null,
                                    tint = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    f,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

/** Şarkı silme onayı. */
@Composable
fun DeleteSongDialog(
    strings: Strings,
    song: AudioFile,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            Button({ onConfirm() }, modifier = Modifier.buttonShadow()) {
                Text(strings.get("delete"), color = Color.Red)
            }
        },
        dismissButton = { TextButton({ onDismiss() }, modifier = Modifier.buttonShadow()) { Text(strings.get("cancel")) } },
        title = { Text(strings.get("delete_song")) },
        text = { Text("\"${song.title}\" ${strings.get("delete_confirm")}") }
    )
}
