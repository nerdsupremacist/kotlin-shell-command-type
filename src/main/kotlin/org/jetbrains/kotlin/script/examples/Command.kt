package org.jetbrains.kotlin.script.examples

@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class Command(val name: String)