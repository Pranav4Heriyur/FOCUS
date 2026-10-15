package com.pranav.focus

import com.pranav.focus.data.*
import org.junit.Assert.*
import org.junit.Test

class MuscleMapTest {
    @Test fun thresholds() {
        assertEquals(MuscleStatus.UNKNOWN, WeeklyMuscleLoad(null).status)
        assertEquals(MuscleStatus.UNDER_TARGET, WeeklyMuscleLoad(0).status)
        assertEquals(MuscleStatus.UNDER_TARGET, WeeklyMuscleLoad(9).status)
        assertEquals(MuscleStatus.ON_TARGET, WeeklyMuscleLoad(10).status)
        assertEquals(MuscleStatus.ON_TARGET, WeeklyMuscleLoad(20).status)
        assertEquals(MuscleStatus.OVER_TARGET, WeeklyMuscleLoad(21).status)
    }
    @Test fun everyRegionHasHitTarget() {
        for (r in MuscleAtlas.regions) {
            var found = false
            var y = 0f
            while (y < 205f && !found) { var x = 50f; while (x < 100f && !found) { if (MuscleAtlas.hit(r.face, x, y)?.key == r.key) found = true; x += 0.5f }; y += 0.5f }
            assertTrue("${r.key} is fully covered by other regions or empty", found)
        }
    }
    @Test fun regionKeysUnique() = assertEquals(MuscleAtlas.regions.size, MuscleAtlas.regions.map { it.key }.toSet().size)
    @Test fun legsAreOnCorrectSegment() {
        for (r in MuscleAtlas.regions.filter { it.group in setOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS) })
            assertTrue(r.key, r.poly.all { it.second in 94f..150f })
        for (r in MuscleAtlas.regions.filter { it.group in setOf(MuscleGroup.CALVES, MuscleGroup.TIBIALIS) })
            assertTrue(r.key, r.poly.all { it.second in 150f..195f })
        assertTrue(MuscleAtlas.regions.filter { it.group == MuscleGroup.QUADRICEPS }.all { it.face == BodyFace.FRONT })
        assertTrue(MuscleAtlas.regions.filter { it.group == MuscleGroup.HAMSTRINGS }.all { it.face == BodyFace.BACK })
    }
    @Test fun mirroredHit() {
        val r = MuscleAtlas.regions.first { it.key == "pec_upper" }
        assertTrue(r.contains(55f, 40f)); assertTrue(r.contains(45f, 40f))
        assertEquals("pec_upper", MuscleAtlas.hit(BodyFace.FRONT, 45f, 40f)?.key)
    }
    @Test fun adapterKeys() {
        assertEquals(MuscleGroup.QUADRICEPS, MuscleGroup.fromKey("quads")); assertNull(MuscleGroup.fromKey("nope"))
    }
}
