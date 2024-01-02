package io.github.vshnv.sprint.parser

interface EvaluationContext {
    fun assign(name: String, value: Any)
    fun fetchValue(name: String): Any
    fun isAssigned(name: String): Boolean
    fun createSubContext(): EvaluationContext
}