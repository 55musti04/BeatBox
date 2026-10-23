package com.blackstack.beatbox.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackstack.beatbox.ui.theme.BeatBoxMotion

/** Alt navigasyondaki tek bir sekme. */
data class BeatBoxNavItem(
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val label: String
)

/**
 * Aurora Glass alt navigasyonu.
 *
 * Seçili sekmede bir pill belirir, ikon dolu varyantına morph yapar ve
 * etiket yumuşakça açılır. Seçim değişiminde hafif bir dokunsal geri bildirim
 * verilir. Yüzey, üst çubukla aynı cam dilini kullanır.
 */
@Composable
fun BeatBoxBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    items: List<BeatBoxNavItem>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.10f)
                    )
                ),
                shape = RoundedCornerShape(30.dp)
            ),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.76f),
        shadowElevation = 18.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                BeatBoxNavItemView(
                    item = item,
                    selected = selectedTab == index,
                    onSelect = { onTabSelected(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BeatBoxNavItemView(
    item: BeatBoxNavItem,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    // Secim hem dokunmayla hem de TalkBack eylemiyle ayni yoldan gecer.
    val select = {
        if (!selected) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSelect()
        }
    }
    val pill by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = BeatBoxMotion.Soft,
        label = "nav_pill"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.94f,
        animationSpec = BeatBoxMotion.Snappy,
        label = "nav_icon_scale"
    )
    val primary = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            // Etiket, secili durum ve tiklama tek bir erisilebilirlik dugumunde
            // toplanir; aksi halde TalkBack sekme adini veya "secili" bilgisini
            // ayri dugumlerde gorur.
            .clearAndSetSemantics {
                contentDescription = item.label
                this.selected = selected
                role = Role.Tab
                onClick {
                    select()
                    true
                }
            }
            .selectable(
                selected = selected,
                onClick = select,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        contentAlignment = Alignment.Center
    ) {
        // Seçili sekmenin arkasındaki pill.
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 2.dp, vertical = 5.dp)
                .graphicsLayer {
                    alpha = pill
                    scaleX = 0.90f + pill * 0.10f
                    scaleY = 0.90f + pill * 0.10f
                }
                .background(primary.copy(alpha = 0.17f), RoundedCornerShape(21.dp))
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            AnimatedContent(
                targetState = selected,
                transitionSpec = {
                    (scaleIn(initialScale = 0.72f) + fadeIn(tween(140))) togetherWith
                        (scaleOut(targetScale = 0.72f) + fadeOut(tween(100)))
                },
                label = "nav_icon_morph"
            ) { isSelected ->
                Icon(
                    imageVector = if (isSelected) item.selectedIcon else item.icon,
                    contentDescription = null,
                    tint = if (isSelected) primary else idle,
                    modifier = Modifier
                        .size(23.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                )
            }

            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(tween(180)) + expandHorizontally(tween(220)),
                exit = fadeOut(tween(120)) + shrinkHorizontally(tween(160))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = item.label,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = primary,
                        maxLines = 1,
                        modifier = Modifier.clearAndSetSemantics { }
                    )
                }
            }
        }
    }
}
