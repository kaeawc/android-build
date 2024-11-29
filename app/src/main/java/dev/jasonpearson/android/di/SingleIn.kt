package dev.jasonpearson.android.di

import javax.inject.Scope
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.reflect.KClass

@Scope
@Retention(RUNTIME) annotation class SingleIn(val scope: KClass<*>)
