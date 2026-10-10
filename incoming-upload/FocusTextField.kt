package com.focus.app.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.focus.app.design.color.FocusColors
import com.focus.app.design.shape.FocusShapes
import com.focus.app.design.typography.FocusType

/** 52 dp single-line field: #1C1C1E, 16 dp radius, white 10% border, 70% when focused. */
@Composable
fun FocusTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Done,
) {
    var focused by remember { mutableStateOf(false) }
    val border = if (focused) FocusColors.FocusBorder else FocusColors.Hairline

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(FocusShapes.Field)
            .background(FocusColors.Inset)
            .border(1.dp, border, FocusShapes.Field)
            .onFocusChanged { focused = it.isFocused }
            .padding(horizontal = 16.dp),
        singleLine = true,
        textStyle = FocusType.TextField,
        cursorBrush = SolidColor(Color.White),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        decorationBox = { inner ->
            Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    FocusText(hint, style = FocusType.FieldHint, color = FocusColors.Tertiary, maxLines = 1)
                }
                inner()
            }
        },
    )
}
