package org.junit
object Assert {
    fun assertEquals(expected: Any?, actual: Any?) { if (expected != actual) throw AssertionError("expected <$expected> but was <$actual>") }
    fun assertEquals(expected: Double, actual: Double, delta: Double) { if (Math.abs(expected - actual) > delta) throw AssertionError("expected <$expected> but was <$actual>") }
    fun assertTrue(c: Boolean) { if (!c) throw AssertionError("expected true") }
    fun assertFalse(c: Boolean) { if (c) throw AssertionError("expected false") }
    fun assertNull(v: Any?) { if (v != null) throw AssertionError("expected null but was <$v>") }
    fun assertNotNull(v: Any?) { if (v == null) throw AssertionError("expected not null") }
    fun fail(msg: String) { throw AssertionError(msg) }
}
