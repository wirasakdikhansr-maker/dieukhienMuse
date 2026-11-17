package com.videodl.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Bg1 = Color(0xFF0E0B24)
val Bg2 = Color(0xFF1B1747)
val Bg3 = Color(0xFF0B1B33)
val Acc1 = Color(0xFF8B5CF6)
val Acc2 = Color(0xFF22D3EE)
val CardBg = Color(0x14FFFFFF)
val CardStroke = Color(0x26FFFFFF)
val TextDim = Color(0xB3FFFFFF)
val Danger = Color(0xFFFF6B6B)
val Ok = Color(0xFF4ADE80)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Acc1,
            secondary = Acc2,
            background = Bg1,
            surface = Bg2,
            onPrimary = Color.White,
            onSurface = Color.White,
            onBackground = Color.White,
            error = Danger
        ),
        content = content
    )
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardBg)
            .border(BorderStroke(1.dp, CardStroke), shape)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(Acc1, Acc2)), alpha = if (enabled) 1f else 0.35f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
