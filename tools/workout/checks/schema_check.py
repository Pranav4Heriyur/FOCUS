#!/usr/bin/env python3
"""Extracts STATEMENTS from WorkoutSchema.kt and verifies the schema behaves as documented.

Runs on Python's sqlite3 (same SQL dialect family as Android's SQLite). This does NOT replace an on-device test:
it checks the DDL, constraints, cascades and reference rules, not the Kotlin adapter code.
"""
import re, sqlite3, sys, pathlib

src = pathlib.Path(__file__).resolve().parents[3] / "app/src/main/java/com/pranav/focus/workout/data/WorkoutSchema.kt"
text = src.read_text()
block = text[text.index("STATEMENTS"):]
stmts = [re.sub(r"\s+", " ", m.strip()) for m in re.findall(r'"""(.*?)"""|"((?:[^"\\]|\\.)*)"', block, re.S)
         for m in [m[0] or m[1]] if m.strip()]
stmts = [s for s in stmts if s.upper().startswith(("CREATE", "INSERT"))]
assert len(stmts) >= 10, f"expected schema statements, got {len(stmts)}"

db = sqlite3.connect(":memory:")
db.execute("PRAGMA foreign_keys = ON")
for s in stmts:
    db.execute(s)
db.execute("INSERT INTO exercise VALUES ('builtin:bench_press','Bench press','PECTORALS',0)")
db.execute("INSERT INTO workout_template VALUES ('t1','Push A','',1,1,NULL)")
db.execute("INSERT INTO template_exercise VALUES ('r1','t1','builtin:bench_press',0,3,8,12,NULL,90,NULL,'')")
db.execute("INSERT INTO workout_routine VALUES ('rt1','Split','',NULL,NULL,'DRAFT',1,1)")
db.execute("INSERT INTO routine_day VALUES ('d1','rt1',0,'Push',0,'t1',1)")
db.execute("INSERT INTO workout_session VALUES ('s1','2026-10-12','t1','Push A','rt1','d1','COMPLETED',1,2)")
db.execute("INSERT INTO session_exercise VALUES ('se1','s1','builtin:bench_press','Bench press',0,3,8,12,NULL,90,NULL,'')")
db.execute("INSERT INTO logged_set VALUES ('ls1','s1','se1',0,'NORMAL',8,60.0,8.0,3)")
db.commit()

def expect_fail(label, sql, params=()):
    try:
        db.execute(sql, params); db.commit()
    except sqlite3.DatabaseError:
        db.rollback(); return
    raise AssertionError(f"FAILED (should be rejected): {label}")

def expect_ok(label, sql, params=()):
    try:
        db.execute(sql, params); db.commit()
    except sqlite3.DatabaseError as e:
        raise AssertionError(f"FAILED (should succeed): {label}: {e}")

# 1. A template referenced by a routine day cannot be deleted (NO ACTION).
expect_fail("delete referenced template", "DELETE FROM workout_template WHERE id='t1'")
# 2. Template still exists after the rejected delete.
assert db.execute("SELECT COUNT(*) FROM workout_template").fetchone()[0] == 1, "template vanished"

# 3. Unreferenced template deletes and cascades its exercise rows.
expect_ok("insert unreferenced template", "INSERT INTO workout_template VALUES ('t2','Old','',1,1,NULL)")
expect_ok("insert row for unreferenced", "INSERT INTO template_exercise VALUES ('r2','t2','builtin:bench_press',0,3,8,12,NULL,90,NULL,'')")
expect_ok("delete unreferenced template", "DELETE FROM workout_template WHERE id='t2'")
assert db.execute("SELECT COUNT(*) FROM template_exercise WHERE template_id='t2'").fetchone()[0] == 0, "cascade failed"

# 4. Sessions are historical: they keep their snapshot after the template row is gone (no FK on session.template_id).
expect_ok("delete routine day reference", "UPDATE routine_day SET template_id=NULL WHERE id='d1'")
expect_ok("delete template after unlinking", "DELETE FROM workout_template WHERE id='t1'")
row = db.execute("SELECT template_name, template_id FROM workout_session WHERE id='s1'").fetchone()
assert row == ("Push A", "t1"), f"session snapshot lost: {row}"
assert db.execute("SELECT COUNT(*) FROM logged_set WHERE session_id='s1'").fetchone()[0] == 1, "sets lost"

# 5. Deleting a routine keeps sessions (session.routine_id is a plain reference).
expect_ok("delete routine", "DELETE FROM workout_routine WHERE id='rt1'")
assert db.execute("SELECT COUNT(*) FROM workout_session WHERE id='s1'").fetchone()[0] == 1, "session deleted with routine"
assert db.execute("SELECT COUNT(*) FROM routine_day WHERE routine_id='rt1'").fetchone()[0] == 0, "routine days not cascaded"

# 6. Deleting a session cascades sets and session exercises.
expect_ok("delete session", "DELETE FROM workout_session WHERE id='s1'")
assert db.execute("SELECT COUNT(*) FROM logged_set").fetchone()[0] == 0, "sets not cascaded"
assert db.execute("SELECT COUNT(*) FROM session_exercise").fetchone()[0] == 0, "session exercises not cascaded"

# 7. Constraint checks. Each case uses valid foreign keys so it fails only for the intended reason.
db.execute("INSERT INTO workout_template VALUES ('t3','X','',1,1,NULL)")
db.execute("INSERT INTO workout_session VALUES ('s9','2026-10-12','t3','X',NULL,NULL,'IN_PROGRESS',1,NULL)")
db.execute("INSERT INTO session_exercise VALUES ('se9','s9','builtin:bench_press','Bench press',0,3,8,12,NULL,90,NULL,'')")
db.execute("INSERT INTO logged_set VALUES ('ls9','s9','se9',0,'NORMAL',8,60.0,8.0,1)")
db.commit()
expect_fail("reps=0 logged", "INSERT INTO logged_set VALUES ('b1','s9','se9',1,'NORMAL',0,60,8,1)")
expect_fail("duplicate set index in same exercise", "INSERT INTO logged_set VALUES ('b2','s9','se9',0,'NORMAL',8,60,8,1)")
expect_fail("bad session status", "INSERT INTO workout_session VALUES ('s10','2026-10-12',NULL,'X',NULL,NULL,'DONE',1,NULL)")
expect_fail("rep range inverted", "INSERT INTO template_exercise VALUES ('rx','t3','builtin:bench_press',0,3,12,8,NULL,90,NULL,'')")
expect_fail("load over 1000", "INSERT INTO template_exercise VALUES ('ry','t3','builtin:bench_press',1,3,8,12,1500,90,NULL,'')")
expect_ok("routine for constraint test", "INSERT INTO workout_routine VALUES ('rtx','R','',NULL,NULL,'DRAFT',1,1)")
expect_fail("rest day with template", "INSERT INTO routine_day VALUES ('dx','rtx',0,'Rest',1,'t3',NULL)")
expect_ok("valid first row", "INSERT INTO template_exercise VALUES ('rw','t3','builtin:bench_press',0,3,8,12,NULL,90,NULL,'')")
expect_fail("duplicate template order index", "INSERT INTO template_exercise VALUES ('rz','t3','builtin:bench_press',0,3,8,12,NULL,90,NULL,'')")
expect_fail("unknown exercise id", "INSERT INTO template_exercise VALUES ('rq','t3','builtin:missing',5,3,8,12,NULL,90,NULL,'')")
# 8. Custom exercise flag must be 0 or 1.
expect_fail("bad is_custom", "INSERT INTO exercise VALUES ('custom:z','Z','',2)")
# 9. Built-in and custom exercises coexist.
expect_ok("custom exercise", "INSERT INTO exercise VALUES ('custom:z','Z','TRICEPS',1)")

print("SCHEMA CHECKS PASSED: statements =", len(stmts))
