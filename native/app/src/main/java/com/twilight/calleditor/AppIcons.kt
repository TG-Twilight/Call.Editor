package com.twilight.calleditor

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

enum class AppIcon { Calls, Backup, Restore, Settings, Search, Add, Refresh, Close, Incoming, Outgoing, Other, Info, Forward, Back }

private val vectors = AppIcon.entries.associateWith { icon ->
    ImageVector.Builder(icon.name, 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black), pathFillType = if (icon == AppIcon.Settings) PathFillType.NonZero else PathFillType.EvenOdd) {
            when (icon) {
                AppIcon.Restore -> { moveTo(13f,3f); curveTo(8f,3f,4f,7f,4f,12f); lineTo(1f,12f); lineTo(5f,16f); lineTo(9f,12f); lineTo(6f,12f); curveTo(6f,8.1f,9.1f,5f,13f,5f); curveTo(16.9f,5f,20f,8.1f,20f,12f); curveTo(20f,15.9f,16.9f,19f,13f,19f); curveTo(11.1f,19f,9.3f,18.2f,8f,16.9f); lineTo(6.6f,18.3f); curveTo(8.2f,20f,10.5f,21f,13f,21f); curveTo(18f,21f,22f,17f,22f,12f); curveTo(22f,7f,18f,3f,13f,3f); close(); moveTo(12f,8f); lineTo(14f,8f); lineTo(14f,12f); lineTo(17f,13.8f); lineTo(16f,15.5f); lineTo(12f,13f); close() }
                AppIcon.Info -> { moveTo(12f,2f); curveTo(6.5f,2f,2f,6.5f,2f,12f); curveTo(2f,17.5f,6.5f,22f,12f,22f); curveTo(17.5f,22f,22f,17.5f,22f,12f); curveTo(22f,6.5f,17.5f,2f,12f,2f); close(); moveTo(11f,10f); lineTo(13f,10f); lineTo(13f,18f); lineTo(11f,18f); close(); moveTo(11f,6f); lineTo(13f,6f); lineTo(13f,8f); lineTo(11f,8f); close() }
                AppIcon.Forward -> { moveTo(9f,5f); lineTo(16f,12f); lineTo(9f,19f); lineTo(7.5f,17.5f); lineTo(13f,12f); lineTo(7.5f,6.5f); close() }
                AppIcon.Back -> { moveTo(20f,11f); lineTo(7.8f,11f); lineTo(13.4f,5.4f); lineTo(12f,4f); lineTo(4f,12f); lineTo(12f,20f); lineTo(13.4f,18.6f); lineTo(7.8f,13f); lineTo(20f,13f); close() }
                AppIcon.Calls -> { moveTo(6.6f,10.8f); curveTo(8.1f,13.7f,10.3f,15.9f,13.2f,17.4f); lineTo(15.4f,15.2f); curveTo(15.7f,14.9f,16.1f,14.8f,16.5f,15f); curveTo(17.7f,15.4f,19f,15.6f,20.3f,15.6f); lineTo(21f,16.3f); lineTo(21f,20f); curveTo(21f,20.6f,20.6f,21f,20f,21f); curveTo(10.6f,21f,3f,13.4f,3f,4f); lineTo(4f,3f); lineTo(7.7f,3f); lineTo(8.4f,3.7f); curveTo(8.4f,5f,8.6f,6.3f,9f,7.5f); lineTo(8.8f,8.6f); close() }
                AppIcon.Backup -> { moveTo(3f,3f); lineTo(21f,3f); lineTo(21f,8f); lineTo(20f,8f); lineTo(20f,21f); lineTo(4f,21f); lineTo(4f,8f); lineTo(3f,8f); close(); moveTo(5f,5f); lineTo(5f,6f); lineTo(19f,6f); lineTo(19f,5f); close(); moveTo(6f,8f); lineTo(6f,19f); lineTo(18f,19f); lineTo(18f,8f); close(); moveTo(9f,11f); lineTo(15f,11f); lineTo(15f,13f); lineTo(9f,13f); close() }
                AppIcon.Settings -> { moveTo(4f,5f); lineTo(20f,5f); lineTo(20f,7f); lineTo(4f,7f); close(); moveTo(4f,11f); lineTo(20f,11f); lineTo(20f,13f); lineTo(4f,13f); close(); moveTo(4f,17f); lineTo(20f,17f); lineTo(20f,19f); lineTo(4f,19f); close(); moveTo(7f,3f); lineTo(11f,3f); lineTo(11f,9f); lineTo(7f,9f); close(); moveTo(14f,9f); lineTo(18f,9f); lineTo(18f,15f); lineTo(14f,15f); close(); moveTo(7f,15f); lineTo(11f,15f); lineTo(11f,21f); lineTo(7f,21f); close() }
                AppIcon.Search -> { moveTo(10f,3f); curveTo(6.1f,3f,3f,6.1f,3f,10f); curveTo(3f,13.9f,6.1f,17f,10f,17f); curveTo(11.6f,17f,13.1f,16.5f,14.3f,15.5f); lineTo(20f,21.2f); lineTo(21.2f,20f); lineTo(15.5f,14.3f); curveTo(16.5f,13.1f,17f,11.6f,17f,10f); curveTo(17f,6.1f,13.9f,3f,10f,3f); close(); moveTo(10f,5f); curveTo(12.8f,5f,15f,7.2f,15f,10f); curveTo(15f,12.8f,12.8f,15f,10f,15f); curveTo(7.2f,15f,5f,12.8f,5f,10f); curveTo(5f,7.2f,7.2f,5f,10f,5f); close() }
                AppIcon.Add -> { moveTo(11f,5f); lineTo(13f,5f); lineTo(13f,11f); lineTo(19f,11f); lineTo(19f,13f); lineTo(13f,13f); lineTo(13f,19f); lineTo(11f,19f); lineTo(11f,13f); lineTo(5f,13f); lineTo(5f,11f); lineTo(11f,11f); close() }
                AppIcon.Refresh -> { moveTo(19f,8f); lineTo(19f,3f); lineTo(17f,5f); curveTo(11f,0f,3f,4f,3f,12f); curveTo(3f,17f,7f,21f,12f,21f); curveTo(16f,21f,20f,18f,21f,14f); lineTo(19f,14f); curveTo(18f,17f,15f,19f,12f,19f); curveTo(8f,19f,5f,16f,5f,12f); curveTo(5f,6f,11f,3f,15.5f,6.5f); lineTo(14f,8f); close() }
                AppIcon.Close -> { moveTo(6f,4.6f); lineTo(12f,10.6f); lineTo(18f,4.6f); lineTo(19.4f,6f); lineTo(13.4f,12f); lineTo(19.4f,18f); lineTo(18f,19.4f); lineTo(12f,13.4f); lineTo(6f,19.4f); lineTo(4.6f,18f); lineTo(10.6f,12f); lineTo(4.6f,6f); close() }
                AppIcon.Incoming -> { moveTo(5f,5f); lineTo(7f,5f); lineTo(7f,15f); lineTo(18f,4f); lineTo(20f,6f); lineTo(9f,17f); lineTo(19f,17f); lineTo(19f,19f); lineTo(5f,19f); close() }
                AppIcon.Outgoing -> { moveTo(5f,5f); lineTo(19f,5f); lineTo(19f,19f); lineTo(17f,19f); lineTo(17f,9f); lineTo(6f,20f); lineTo(4f,18f); lineTo(15f,7f); lineTo(5f,7f); close() }
                AppIcon.Other -> { moveTo(4f,11f); lineTo(8f,11f); lineTo(8f,13f); lineTo(4f,13f); close(); moveTo(10f,11f); lineTo(14f,11f); lineTo(14f,13f); lineTo(10f,13f); close(); moveTo(16f,11f); lineTo(20f,11f); lineTo(20f,13f); lineTo(16f,13f); close() }
            }
        }
    }.build()
}

@Composable
fun AppSymbol(icon: AppIcon, description: String? = null, modifier: Modifier = Modifier) {
    Icon(vectors.getValue(icon), contentDescription = description, modifier = modifier)
}
