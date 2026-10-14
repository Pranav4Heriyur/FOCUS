package org.junit
@Retention(AnnotationRetention.RUNTIME)
annotation class Test(val expected: kotlin.reflect.KClass<out Throwable> = Test.None::class) {
    class None : Throwable()
}
