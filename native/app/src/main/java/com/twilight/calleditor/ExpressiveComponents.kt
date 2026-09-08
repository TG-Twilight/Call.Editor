package com.twilight.calleditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Large outside corners and small shared corners make related rows read as one group. */
fun connectedShape(first: Boolean = true, last: Boolean = true) = RoundedCornerShape(
    topStart = if (first) 28.dp else 6.dp, topEnd = if (first) 28.dp else 6.dp,
    bottomStart = if (last) 28.dp else 6.dp, bottomEnd = if (last) 28.dp else 6.dp
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveBadge(icon: AppIcon, modifier: Modifier = Modifier, prominent: Boolean = false) {
    Surface(modifier = modifier.size(if (prominent) 80.dp else 48.dp),
        shape = if (prominent) MaterialShapes.Cookie9Sided.toShape() else MaterialTheme.shapes.large,
        color = if (prominent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (prominent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer) {
        Box(contentAlignment = Alignment.Center) { AppSymbol(icon, modifier = Modifier.size(if (prominent) 36.dp else 24.dp)) }
    }
}

/** Keep settings visibly tinted while retaining the elevated surface base. */
@Composable
fun settingsContainerColor(): Color = lerp(
    MaterialTheme.colorScheme.surfaceContainerHighest,
    MaterialTheme.colorScheme.primaryContainer,
    0.6f
)

@Composable
fun ThemeChoices(value: String, enabled: Boolean, onSelect: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth().selectableGroup()
        .clip(RoundedCornerShape(20.dp)).background(colors.surfaceContainerLow)
        .padding(4.dp).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色").forEach { (key, title) ->
            val selected = value == key
            Box(modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) colors.primary.copy(alpha = if (enabled) 1f else 0.38f) else Color.Transparent)
                .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = { onSelect(key) })
                .padding(horizontal = 4.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                Text(title, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center,
                    color = if (selected) colors.onPrimary else colors.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.38f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveAction(text: String, enabled: Boolean = true, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val height = ButtonDefaults.MediumContainerHeight
    Button(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = height),
        shapes = ButtonDefaults.shapesFor(height), contentPadding = ButtonDefaults.contentPaddingFor(height),
        colors = ButtonDefaults.buttonColors()) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}
