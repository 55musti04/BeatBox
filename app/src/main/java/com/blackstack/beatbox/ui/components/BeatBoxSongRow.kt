package com.blackstack.beatbox.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackstack.beatbox.AlbumArtImage
import com.blackstack.beatbox.AnimatedEqualizer
import com.blackstack.beatbox.AudioFile
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.formatTime
import com.blackstack.beatbox.ui.theme.BeatBoxMotion
import com.blackstack.beatbox.ui.theme.LocalReducedMotion

/**
 * Kütüphane ve çalma listelerinde kullanılan ortak Aurora Glass şarkı satırı.
 *
 * Sola kaydırma favori ekler/çıkarır, sağa kaydırma silme onayını açar.
 * Seçim modunda satır, tek dokunuşla seçimi değiştirir.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun BeatBoxSongRow(
    audio: AudioFile,
    strings: Strings,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    isSelected: Boolean,
    selectionMode: Boolean,
    compact: Boolean,
    onFavClick: () -> Unit,
    onDelete: () -> Unit,
    onRowClick: () -> Unit,
    onRowLongClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(if (compact) 16.dp else 20.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotion.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) 0.985f else 1f,
        animationSpec = if (reducedMotion) tween(0) else BeatBoxMotion.Snappy,
        label = "library_row_press"
    )

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> if (!selectionMode) onDelete()
                SwipeToDismissBoxValue.EndToStart -> if (!selectionMode) onFavClick()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            // Satır yerinde kalsın; eylem diyalog veya durum güncellemesiyle yürür.
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !selectionMode,
        enableDismissFromEndToStart = !selectionMode,
        backgroundContent = {
            val deleting = dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd
            val tint = if (deleting) MaterialTheme.colorScheme.error else secondary
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 3.dp)
                    .clip(shape)
                    .background(tint.copy(alpha = 0.18f))
                    .clearRowSemantics(),
                contentAlignment = if (deleting) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        imageVector = if (deleting) Icons.Rounded.Delete else {
                            if (isFavorite) Icons.Rounded.FavoriteBorder else Icons.Rounded.Favorite
                        },
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = if (deleting) strings.get("delete") else {
                            if (isFavorite) strings.get("remove_from_favorites") else strings.get("add_to_favorites")
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = tint
                    )
                }
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 3.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(shape)
                .background(
                    if (isCurrent) {
                        Brush.linearGradient(
                            listOf(
                                primary.copy(alpha = 0.16f),
                                secondary.copy(alpha = 0.07f),
                                Color.White.copy(alpha = 0.035f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.055f),
                                Color.White.copy(alpha = 0.028f)
                            )
                        )
                    }
                )
                .border(
                    width = if (isCurrent || isSelected) 1.2.dp else 1.dp,
                    brush = if (isCurrent || isSelected) {
                        Brush.linearGradient(
                            listOf(
                                primary.copy(alpha = 0.85f),
                                secondary.copy(alpha = 0.45f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.09f),
                                Color.White.copy(alpha = 0.03f)
                            )
                        )
                    },
                    shape = shape
                )
                .clearAndSetSemantics {
                    contentDescription = buildString {
                        append(audio.title)
                        append(", ")
                        append(audio.artist)
                        append(", ")
                        append(formatTime(audio.duration))
                        if (isFavorite) {
                            append(", ")
                            append(strings.get("favorites"))
                        }
                    }
                    role = Role.Button
                    if (selectionMode) {
                        selected = isSelected
                        if (isSelected) stateDescription = strings.get("selected")
                    }
                    onClick {
                        onRowClick()
                        true
                    }
                    onLongClick {
                        onRowLongClick()
                        true
                    }
                }
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onRowClick,
                    onLongClick = onRowLongClick
                )
                .padding(
                    horizontal = 10.dp,
                    vertical = if (compact) 6.dp else 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val artSize = if (compact) 44.dp else 56.dp
            Box(
                modifier = Modifier
                    .size(artSize)
                    .clip(RoundedCornerShape(if (compact) 11.dp else 14.dp))
            ) {
                AlbumArtImage(audio, Modifier.fillMaxSize(), isHQ = !compact)
                if (isCurrent && isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.42f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedEqualizer(isPlaying = true, color = primary)
                    }
                }
                if (selectionMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = if (isSelected) 0.46f else 0.24f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) primary else Color.Black.copy(alpha = 0.55f)
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.width(if (compact) 10.dp else 12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = audio.title,
                        fontSize = if (compact) 13.sp else 14.5.sp,
                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                        color = if (isCurrent) primary else onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (audio.isExplicit) {
                        Spacer(Modifier.width(5.dp))
                        com.blackstack.beatbox.ExplicitBadge()
                    }
                }
                Spacer(Modifier.height(if (compact) 1.dp else 3.dp))
                Text(
                    text = audio.artist,
                    fontSize = if (compact) 10.5.sp else 11.5.sp,
                    color = onSurface.copy(alpha = 0.56f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatTime(audio.duration),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = onSurface.copy(alpha = 0.45f)
                )
                if (isFavorite) {
                    Spacer(Modifier.height(3.dp))
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = secondary.copy(alpha = 0.9f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

private fun Modifier.clearRowSemantics(): Modifier = this.then(
    Modifier.clearAndSetSemantics { }
)

