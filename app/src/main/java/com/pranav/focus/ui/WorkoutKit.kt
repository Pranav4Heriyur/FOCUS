package com.pranav.focus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranav.focus.R

/** Compact numeric/text input for set rows and prescriptions. 48 dp minimum touch height. */
@Composable
fun WField(
    value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, hint: String = "", numeric: Boolean = true,
    decimal: Boolean = true, align: TextAlign = TextAlign.Center, desc: String = hint,
) {
    BasicTextField(
        value = value, onValueChange = onChange, singleLine = true,
        textStyle = TextStyle(color = C.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, textAlign = align, fontFamily = Inter),
        cursorBrush = SolidColor(C.White),
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) (if (decimal) KeyboardType.Decimal else KeyboardType.Number) else KeyboardType.Text),
        modifier = modifier.semantics { contentDescription = desc },
        decorationBox = { inner ->
            Box(Modifier.defaultMinSize(minHeight = 48.dp).clip(RoundedCornerShape(12.dp)).background(C.Sheet).padding(horizontal = 10.dp), contentAlignment = if (align == TextAlign.Center) Alignment.Center else Alignment.CenterStart) {
                if (value.isEmpty()) Text(hint, color = C.Text3, fontSize = 16.sp)
                inner()
            }
        },
    )
}

@Composable
fun WChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.defaultMinSize(minHeight = 40.dp).clip(CircleShape).background(if (selected) C.White else C.Raised)
            .border(1.dp, if (selected) C.White else C.Hairline, CircleShape).clickable { onClick() }.padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (selected) C.Black else C.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1) }
}

@Composable
fun WIconBtn(icon: Int, desc: String, tint: Color = C.White, size: Dp = 48.dp, onClick: () -> Unit) {
    Box(Modifier.size(size).clip(CircleShape).clickable { onClick() }.semantics { contentDescription = desc }, contentAlignment = Alignment.Center) { Ico(icon, 22.dp, tint) }
}

@Composable
fun WConfirm(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit, extraLabel: String? = null, onExtra: (() -> Unit)? = null) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = C.Sheet, titleContentColor = C.White, textContentColor = C.Text2,
        title = { Text(title, fontWeight = FontWeight.SemiBold) }, text = { Text(text) },
        confirmButton = { Text(confirm, color = C.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onConfirm() }.padding(12.dp)) },
        dismissButton = {
            Row {
                if (extraLabel != null && onExtra != null) Text(extraLabel, color = C.White, modifier = Modifier.clickable { onExtra() }.padding(12.dp))
                Text("Cancel", color = C.Text2, modifier = Modifier.clickable { onDismiss() }.padding(12.dp))
            }
        },
    )
}

@Composable
fun WEmpty(title: String, body: String, action: String? = null, onAction: (() -> Unit)? = null) {
    FocusCard(Modifier.fillMaxWidth()) {
        Text(title, color = C.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        Text(body, color = C.Text2, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = if (action != null) 12.dp else 0.dp))
        if (action != null && onAction != null) GhostButton(action, onClick = onAction)
    }
}

@Composable
fun WSection(title: String, trailing: String? = null, onTrailing: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = C.White, fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, color = C.Text2, fontSize = 13.sp, modifier = Modifier.clickable { onTrailing?.invoke() }.padding(8.dp))
    }
}

fun mmss(totalSec: Long): String { val s = totalSec.coerceAtLeast(0); return "%d:%02d".format(s / 60, s % 60) }
fun wkDate(ts: Long): String = java.time.Instant.ofEpochMilli(ts).atZone(com.pranav.focus.data.AppClock.zone()).toLocalDate().let { "${it.dayOfMonth} ${it.month.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() }}" }
