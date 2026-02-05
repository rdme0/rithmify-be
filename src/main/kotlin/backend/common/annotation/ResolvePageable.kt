package backend.common.annotation

import kotlin.reflect.KClass

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ResolvePageable(val enumClass: KClass<out Enum<*>>)
