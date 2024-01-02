package io.github.vshnv.sprint.parser.ast

import io.github.vshnv.sprint.parser.EvaluationContext

// Binary Operations
// Invocation
data class InvocationNode(val functionGenerator: ExpressionNode, val args: List<ExpressionNode>): ExpressionNode {
    override fun evaluate(context: EvaluationContext): Any {
        val f = functionGenerator.evaluate(context)
        if (f !is Invokable) {
            throw RuntimeException("Invalid Invocation, Invoke applied on non-function type")
        }
        return f.invoke(context, args.map { it.evaluate(context) })
    }
}


data class SourceNode(val nodes: List<Node>): Node {
    override fun execute(context: EvaluationContext) {
        nodes.forEach { it.execute(context) }
    }
}

data class BinaryOperationNode(val lhs: ExpressionNode, val operator: String, val rhs: ExpressionNode): ExpressionNode {
    override fun evaluate(context: EvaluationContext): Any {
        val operatorFunction = context.fetchValue(operator)
        if (operatorFunction !is Invokable) {
            throw RuntimeException("Invalid Invocation, Invoke applied on non-function type")
        }
        return operatorFunction.invoke(context, listOf(lhs, rhs))
    }

    private fun coerce(value: Any): Boolean {
        if (value is Boolean) {
            return value
        }
        return (value != Unit)
    }
}

data class UnaryOperationNode(val operator: String, val operand: ExpressionNode): ExpressionNode {
    override fun evaluate(context: EvaluationContext): Any {
        val operatorFunction = context.fetchValue("${operator}UNARY")
        if (operatorFunction !is Invokable) {
            throw RuntimeException("Invalid Invocation, Invoke applied on non-function type")
        }
        return operatorFunction.invoke(context, listOf(operand))
    }
}

class DataCreateNode(val entries: List<Pair<String, ExpressionNode>>): ExpressionNode {
    override fun evaluate(context: EvaluationContext): Any {
        return entries.toMap().mapValues { it.value.evaluate(context) }
    }

}

fun native(evaluateNative: (EvaluationContext) -> Any) = NativeNode(evaluateNative)

class NativeNode(val evaluateNative: (EvaluationContext) -> Any): ExpressionNode {
    override fun evaluate(context: EvaluationContext): Any {
        return evaluateNative(context)
    }
}

class ConditionalNode(val condition: ExpressionNode, val truthy: ExpressionNode, val falsy: ExpressionNode? = null): ExpressionNode {
    override fun evaluate(context: EvaluationContext): Any {
        val cond = condition.evaluate(context)
        return if (cond == true) {
            val data = truthy.evaluate(context)
            return if (falsy != null) data else {
                Unit
            }
        } else {
            falsy?.evaluate(context) ?: Unit
        }
    }

}