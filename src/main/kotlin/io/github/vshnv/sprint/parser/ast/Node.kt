package io.github.vshnv.sprint.parser.ast

import io.github.vshnv.sprint.parser.EvaluationContext

sealed interface Node {
    fun execute(context: EvaluationContext)
}

sealed interface ExpressionNode: Node {

    override fun execute(context: EvaluationContext) {
        evaluate(context)
    }
    fun evaluate(context: EvaluationContext): Any
}