// Local runner: invokes every @Test method of the given classes, honouring expected exceptions.
// JUnit is not reachable from this sandbox, so this runs the same test code with the shim in tools/workout/shim.
import org.junit.Test
import kotlin.reflect.KClass

fun main(args: Array<String>) {
    var passed = 0
    val failures = mutableListOf<String>()
    for (name in args) {
        val cls = Class.forName(name)
        val instance = cls.getDeclaredConstructor().newInstance()
        for (m in cls.declaredMethods.sortedBy { it.name }) {
            val ann = m.getAnnotation(Test::class.java) ?: continue
            val expected: KClass<out Throwable> = ann.expected
            try {
                m.invoke(instance)
                if (expected != Test.None::class) failures.add("${m.name}: expected ${expected.simpleName}")
                else passed++
            } catch (ite: java.lang.reflect.InvocationTargetException) {
                val cause = ite.cause!!
                if (expected != Test.None::class && expected.java.isInstance(cause)) passed++
                else failures.add("${m.name}: ${cause::class.simpleName}: ${cause.message}")
            }
        }
    }
    println("JVM TESTS PASSED: $passed")
    if (failures.isNotEmpty()) { println("FAILURES:"); failures.forEach { println("  $it") }; kotlin.system.exitProcess(1) }
}
