package com.pranav.focus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Monochrome tokens from the design handoff. */
object C {
    val Black = Color(0xFF000000)
    val Card = Color(0xFF0B0B0C)
    val Raised = Color(0xFF141416)
    val Sheet = Color(0xFF1C1C1E)
    val Track = Color(0xFF2A2A2D)
    val White = Color(0xFFFFFFFF)
    val Text2 = Color(0xFFA1A1A6)
    val Text3 = Color(0xFF636366)
    val Hairline = Color(0x1AFFFFFF)
}

@Composable
fun FocusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.White, onPrimary = C.Black,
            background = C.Black, onBackground = C.White,
            surface = C.Card, onSurface = C.White,
            surfaceVariant = C.Raised, onSurfaceVariant = C.Text2,
            outline = C.Hairline,
        ),
        content = content,
    )
}

@Composable
fun Caps(text: String, modifier: Modifier = Modifier, color: Color = C.Text2) {
    Text(text.uppercase(), modifier = modifier, color = color, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 1.4.sp)
}

@Composable
fun FocusCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .clip(shape)
            .background(C.Card)
            .border(BorderStroke(1.dp, C.Hairline), shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth().height(56.dp)
            .clip(CircleShape)
            .background(if (enabled) C.White else C.Track)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (enabled) C.Black else C.Text3, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth().height(56.dp)
            .clip(CircleShape)
            .border(BorderStroke(1.dp, C.Text3), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = C.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)?) {
    Column(Modifier.padding(top = 16.dp, bottom = 12.dp)) {
        if (onBack != null) {
            Text(
                "‹ Back", color = C.Text2, fontSize = 14.sp,
                modifier = Modifier.clickable { onBack() }.padding(vertical = 8.dp),
            )
        }
        Text(title, color = C.White, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Light)
    }
}
