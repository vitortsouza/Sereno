package com.opencode.sereno.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.opencode.sereno.ui.theme.SerenoColors

@Composable
fun PlaceholderScreen(
    title: String,
    message: String,
    icon: ImageVector,
    onExploreSounds: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(84.dp)
                .background(SerenoColors.Mint.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = SerenoColors.Mint.copy(alpha = 0.85f),
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = SerenoColors.Muted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(26.dp))
        Surface(
            onClick = onExploreSounds,
            shape = RoundedCornerShape(16.dp),
            color = SerenoColors.SurfaceHi
        ) {
            Text(
                "Explorar sons",
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 13.dp),
                style = MaterialTheme.typography.labelLarge,
                color = SerenoColors.Mint
            )
        }
        Spacer(Modifier.weight(1.3f))
    }
}
