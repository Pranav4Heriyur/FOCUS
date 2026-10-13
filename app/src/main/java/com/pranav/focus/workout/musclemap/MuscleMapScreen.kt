package com.pranav.focus.workout.musclemap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.pranav.focus.R
import com.pranav.focus.ui.C

/**
 * Muscle map screen. Renders the supplied anatomy chart and draws the same polygons used for
 * tap hit-testing. Counts come from [source]; with no source they show as Unknown.
 */
@Composable
fun MuscleMapScreen(source: CompletedSetSource = UnavailableCompletedSetSource) {
    var counts by remember { mutableStateOf<Map<MuscleGroup, Int?>>(countsFor(null)) }
    var view by remember { mutableStateOf(MuscleView.FRONT) }
    var selected by remember { mutableStateOf<MuscleGroup?>(null) }

    LaunchedEffect(source) {
        counts = countsFor(source.completedSetsThisWeek())
    }

    val polygons = if (view == MuscleView.FRONT) FRONT_POLYGONS else BACK_POLYGONS
    val groups = MuscleGroup.values().filter { it.view == view && polygons.any { p -> p.group == it } }
    val sel = selected?.takeIf { it.view == view }

    Column(
        Modifier.fillMaxSize().background(C.Black).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Muscle map", color = C.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text("Completed sets this week", color = C.Text2, fontSize = 13.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MuscleViewChip("Front", view == MuscleView.FRONT) { view = MuscleView.FRONT; selected = null }
            MuscleViewChip("Back", view == MuscleView.BACK) { view = MuscleView.BACK; selected = null }
        }

        // Chart on a light card: the supplied chart has a white background.
        Box(
            Modifier.fillMaxWidth().aspectRatio(CHART_WIDTH / CHART_HEIGHT)
                .clip(RoundedCornerShape(16.dp)).background(Color.White)
                .pointerInput(view) {
                    detectTapGestures { offset ->
                        val fit = ArtworkFit.fit(size.width.toFloat(), size.height.toFloat(), CHART_WIDTH, CHART_HEIGHT)
                        selected = hitTest(polygons, fit, offset.x, offset.y)
                    }
                }
        ) {
            Image(
                painter = painterResource(R.drawable.muscle_map_chart),
                contentDescription = "Anatomy chart, ${view.label.lowercase()} view",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            Canvas(Modifier.fillMaxSize()) {
                val fit = ArtworkFit.fit(size.width, size.height, CHART_WIDTH, CHART_HEIGHT)
                for (poly in polygons) {
                    val path = Path()
                    val xy = poly.xy
                    path.moveTo(fit.left + xy[0] * fit.width, fit.top + xy[1] * fit.height)
                    var i = 2
                    while (i < xy.size) {
                        path.lineTo(fit.left + xy[i] * fit.width, fit.top + xy[i + 1] * fit.height)
                        i += 2
                    }
                    path.close()
                    if (poly.group == sel) {
                        drawPath(path, color = Color.Black.copy(alpha = 0.22f))
                        drawPath(path, color = Color.Black, style = Stroke(width = 3.dp.toPx()))
                    } else {
                        drawPath(path, color = Color.Black.copy(alpha = 0.06f))
                    }
                }
            }
        }

        // Accessible alternative to tapping the chart (switch/keyboard users).
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(groups) { g ->
                MuscleViewChip(g.label, g == sel) { selected = g }
            }
        }

        DetailCard(sel, counts)

        Text(
            "Set bands: 0–9 low, 10–20 in range, 21+ high. Only completed sets count. Not medical guidance.",
            color = C.Text3, fontSize = 11.sp
        )
    }
}

@Composable
private fun DetailCard(group: MuscleGroup?, counts: Map<MuscleGroup, Int?>) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.Raised)
            .border(1.dp, C.Hairline, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (group == null) {
            Text("Tap a muscle on the chart", color = C.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("Or choose one from the list above.", color = C.Text2, fontSize = 13.sp)
            return@Column
        }
        Text(group.label, color = C.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(group.description, color = C.Text2, fontSize = 13.sp)
        val sets = counts[group]
        val band = setBandFor(sets)
        if (sets == null) {
            Text("Completed sets this week: Unknown", color = C.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("No workout data source is connected yet.", color = C.Text3, fontSize = 12.sp)
        } else {
            Text("Completed sets this week: $sets", color = C.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("${band!!.label} (${band.range})", color = C.Text2, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MuscleViewChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) C.White else C.Raised)
            .border(1.dp, C.Hairline, RoundedCornerShape(18.dp))
            .semantics {
                contentDescription = "$label${if (selected) ", selected" else ""}"
                role = Role.Button
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) C.Black else C.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
