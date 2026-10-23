package com.blackstack.beatbox.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewCompact
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackstack.beatbox.Strings

// ---------------------------------------------------------------------------
// Kütüphane hero
// ---------------------------------------------------------------------------

@Composable
internal fun LibraryHero(
    songCount: Int,
    favoriteCount: Int,
    totalMinutes: Long,
    strings: Strings,
    onShuffle: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val shape = RoundedCornerShape(30.dp)
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(
                elevation = 20.dp,
                shape = shape,
                ambientColor = primary.copy(alpha = 0.38f),
                spotColor = secondary.copy(alpha = 0.28f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        primary.copy(alpha = 0.30f),
                        secondary.copy(alpha = 0.15f),
                        surfaceVariant.copy(alpha = 0.74f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.22f),
                        primary.copy(alpha = 0.24f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = shape
            )
    ) {
        // Sağ üstteki aurora lekesi.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-48).dp)
                .size(170.dp)
                .background(
                    Brush.radialGradient(
                        listOf(tertiary.copy(alpha = 0.26f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Column(Modifier.padding(20.dp)) {
            Text(
                text = "BEATBOX",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.6.sp,
                color = primary.copy(alpha = 0.95f)
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = strings.get("library"),
                fontSize = 31.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = "$songCount ${strings.get("songs")} · $favoriteCount ${strings.get("favorites")}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(percent = 50))
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = primary.copy(alpha = 0.9f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "$totalMinutes ${strings.get("minutes")}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
                    )
                }

                Spacer(Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(10.dp, CircleShape, ambientColor = primary.copy(alpha = 0.45f))
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(primary, secondary.copy(alpha = 0.92f))
                            )
                        )
                        .clearAndSetSemantics {
                            contentDescription = strings.get("shuffle")
                            role = Role.Button
                            onClick {
                                onShuffle()
                                true
                            }
                        }
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onShuffle()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Filtre çipleri ve seçim çubuğu
// ---------------------------------------------------------------------------

@Composable
internal fun LibraryFilterBar(
    filter: LibraryFilter,
    onFilterChange: (LibraryFilter) -> Unit,
    compact: Boolean,
    onToggleDensity: () -> Unit,
    strings: Strings,
    selectionCount: Int,
    onClearSelection: () -> Unit,
    onPlaySelected: () -> Unit,
    onFavoriteSelected: () -> Unit,
    onQueueSelected: () -> Unit
) {
    val background = MaterialTheme.colorScheme.background
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        background.copy(alpha = 0.99f),
                        background.copy(alpha = 0.94f)
                    )
                )
            )
            .padding(top = 4.dp, bottom = 8.dp)
    ) {
        if (selectionCount > 0) {
            LibrarySelectionBar(
                count = selectionCount,
                strings = strings,
                onClear = onClearSelection,
                onPlay = onPlaySelected,
                onFavorite = onFavoriteSelected,
                onQueue = onQueueSelected
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Yoğunluk düğmesi çiplerin solunda durur; sağdaki A-Z
                // kaydırıcısının dokunma alanıyla çakışmaz.
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.055f))
                        .border(1.dp, Color.White.copy(alpha = 0.07f), CircleShape)
                        .clearAndSetSemantics {
                            contentDescription = if (compact) {
                                strings.get("comfortable_view")
                            } else {
                                strings.get("compact_view")
                            }
                            role = Role.Button
                            onClick {
                                onToggleDensity()
                                true
                            }
                        }
                        .clickable { onToggleDensity() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (compact) Icons.Rounded.ViewAgenda else Icons.Rounded.ViewCompact,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(LibraryFilter.entries.toList(), key = { it.name }) { item ->
                        LibraryFilterChip(
                            label = item.label(strings),
                            selected = item == filter,
                            onClick = { onFilterChange(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySelectionBar(
    count: Int,
    strings: Strings,
    onClear: () -> Unit,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onQueue: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val shape = RoundedCornerShape(22.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(primary.copy(alpha = 0.18f), secondary.copy(alpha = 0.09f))
                )
            )
            .border(1.dp, primary.copy(alpha = 0.30f), shape)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LibraryActionButton(
            icon = Icons.Rounded.Close,
            label = strings.get("close"),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            iconSize = 19.dp,
            onAction = onClear
        )
        Text(
            text = "$count ${strings.get("selected")}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        LibraryActionButton(
            icon = Icons.Rounded.PlayArrow,
            label = strings.get("play"),
            tint = primary,
            iconSize = 22.dp,
            onAction = onPlay
        )
        LibraryActionButton(
            icon = Icons.Rounded.Favorite,
            label = strings.get("add_to_favorites"),
            tint = secondary,
            iconSize = 19.dp,
            onAction = onFavorite
        )
        LibraryActionButton(
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            label = strings.get("add_to_queue"),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            iconSize = 20.dp,
            onAction = onQueue
        )
    }
}

@Composable
private fun LibraryActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    iconSize: androidx.compose.ui.unit.Dp,
    onAction: () -> Unit
) {
    IconButton(
        onClick = onAction,
        modifier = Modifier
            .size(38.dp)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                onClick {
                    onAction()
                    true
                }
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun LibraryFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val shape = RoundedCornerShape(percent = 50)
    val background = if (selected) {
        Brush.linearGradient(
            listOf(primary.copy(alpha = 0.92f), secondary.copy(alpha = 0.76f))
        )
    } else {
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.055f), Color.White.copy(alpha = 0.035f))
        )
    }

    Box(
        modifier = Modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.08f), shape)
                }
            )
            .background(background)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Tab
                this.selected = selected
                onClick {
                    onClick()
                    true
                }
            }
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            },
            maxLines = 1
        )
    }
}

// ---------------------------------------------------------------------------
// Harf başlıkları
// ---------------------------------------------------------------------------

@Composable
internal fun LibraryLetterHeader(letter: Char) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 20.dp, top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = letter.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.4.sp,
            color = primary
        )
        Spacer(Modifier.width(9.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(primary.copy(alpha = 0.24f), Color.White.copy(alpha = 0.03f))
                    )
                )
        )
    }
}

// ---------------------------------------------------------------------------
// A-Z scrubber
// ---------------------------------------------------------------------------

@Composable
internal fun AlphabetScrubber(
    letters: List<Char>,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    if (letters.isEmpty()) return
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val haptic = LocalHapticFeedback.current
    var active by remember { mutableStateOf<Char?>(null) }

    Box(
        modifier = modifier
            .width(30.dp)
            .fillMaxHeight()
            .clearAndSetSemantics { }
            .pointerInputLetters(letters) { letter ->
                onLetterSelected(letter)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            letters.forEach { letter ->
                Text(
                    text = letter.toString(),
                    fontSize = 9.sp,
                    fontWeight = if (letter == active) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (letter == active) {
                        primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.36f)
                    }
                )
            }
        }

        active?.let { letter ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = (-56).dp)
                    .size(50.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        ambientColor = primary.copy(alpha = 0.5f)
                    )
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(primary, secondary.copy(alpha = 0.9f)))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letter.toString(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/**
 * A-Z kaydırıcısının dokunma/sürükleme mantığı.
 *
 * [onLetterChanged] her yeni harfe geçişte bir kez çağrılır; böylece liste
 * sürekli aynı konuma kaydırılmaz.
 */
private fun Modifier.pointerInputLetters(
    letters: List<Char>,
    onLetterChanged: (Char) -> Unit
): Modifier = this.then(
    Modifier.pointerInput(letters) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)

            fun letterAt(y: Float): Char {
                val height = size.height.coerceAtLeast(1)
                val index = ((y / height) * letters.size).toInt().coerceIn(0, letters.lastIndex)
                return letters[index]
            }

            var current = letterAt(down.position.y)
            onLetterChanged(current)

            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                val next = letterAt(change.position.y)
                if (next != current) {
                    current = next
                    onLetterChanged(next)
                }
                change.consume()
            }
        }
    }
)

internal fun LibraryFilter.label(strings: Strings): String = when (this) {
    LibraryFilter.ALL -> strings.get("all")
    LibraryFilter.SONGS -> strings.get("songs")
    LibraryFilter.FAVORITES -> strings.get("favorites")
    LibraryFilter.ALBUMS -> strings.get("albums")
    LibraryFilter.ARTISTS -> strings.get("artists")
    LibraryFilter.FOLDERS -> strings.get("folders")
}
