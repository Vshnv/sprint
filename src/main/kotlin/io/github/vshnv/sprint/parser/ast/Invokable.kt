package io.github.vshnv.sprint.parser.ast

import io.github.vshnv.sprint.parser.EvaluationContext
import io.github.vshnv.sprint.parser.ImmutableEvaluationContext

interface Invokable {
    fun invoke(evaluationContext: EvaluationContext, args: List<Any>): Any
}

data class InvokableFunction(val capturedContext: EvaluationContext? = null, val parameters: List<String>, val functionExpressionNode: ExpressionNode): Invokable {
    override fun invoke(evaluationContext: EvaluationContext, args: List<Any>): Any {
        val functionContext = ImmutableEvaluationContext(capturedContext)
        parameters.zip(args).forEach { (id, exp) ->
            functionContext.assign(id, exp)
        }
        return when {
            args.size > parameters.size -> {
                // Invalid Input
                throw RuntimeException("Too many arguments passed to function")
            }
            args.size < parameters.size -> {
                // Partial Application
                InvokableFunction(functionContext, parameters.subList(args.size, parameters.size), functionExpressionNode)
            }
            else -> {
                // Evaluate
                functionExpressionNode.evaluate(functionContext)
            }
        }
    }
}