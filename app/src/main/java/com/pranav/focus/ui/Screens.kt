package com.pranav.focus.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.pranav.focus.R
import com.pranav.focus.data.Caller
import com.pranav.focus.data.Store
import com.pranav.focus.lock.Allowed
import com.pranav.focus.lock.AppState
import com.pranav.focus.lock.SessionController
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

enum class Screen { Home, Focus, Study, Stats, Profile, Apps, Callers, Setup, Rules, NewRule, TimeBank, Mirror, Jar, Rank, Connections, Security, Workout,
    Plan, PlanSubjects, PlanAdd, PlanEdit, PlanReorder, PlanDetail, PlanCalendar, PlanFocus, PlanSummary, PlanSettings }



// --------------------------------------------------------------------------------------
// Profile + placeholders
// --------------------------------------------------------------------------------------
@Composable
fun ProfileStat(v: String, l: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(v, color = C.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Caps(l)
    }
}

@Composable
fun ProfileRow(icon: Int, title: String, sub: String, onClick: () -> Unit) {
    FocusCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, color = C.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(sub, color = C.Text2, fontSize = 14.sp)
            }
            Ico(R.drawable.ic_chevron_right, 22.dp, C.Text3)
        }
    }
}

@Composable
fun PlaceholderScreen(title: String, note: String) {
    Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 100.dp)) {
        ScreenHeader(title, null)
        FocusCard(Modifier.fillMaxWidth()) {
            Text("Coming next", color = C.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(note, color = C.Text2, fontSize = 14.sp)
        }
    }
}

