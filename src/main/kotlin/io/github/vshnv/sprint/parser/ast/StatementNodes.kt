package io.github.vshnv.sprint.parser.ast

import io.github.vshnv.sprint.parser.EvaluationContext

data class DefineNode(val name: String, val expressionNode: ExpressionNode): Node {
    override fun execute(context: EvaluationContext) {
        if (context.isAssigned(name)) {
            throw RuntimeException("Unable to define already defined variable - $name")
        }
        context.assign(name, expressionNode.evaluate(context))
    }
}

data class AssignNode(val scope: ExpressionNode? = null, val name: String, val expressionNode: ExpressionNode): ExpressionNode {

    override fun evaluate(context: EvaluationContext): Any {
        val scope = scope?.evaluate(context) as? MutableMap<String, Any>
        if (scope != null) {
            val scopedRes = scope.put(name, expressionNode.evaluate(context))
            return scopedRes ?: Unit
        }
        if (!context.isAssigned(name)) {
            throw RuntimeException("Unable to assign to undefined variable - $name")
        }
        val result = expressionNode.evaluate(context)
        context.assign(name, result)
        return result
    }
}