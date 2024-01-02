package io.github.vshnv.sprint.parser.ast

import io.github.vshnv.sprint.parser.EvaluationContext


data class IntNode(val value: Int): ExpressionNode {
    override fun evaluate(context: EvaluationContext): Any {
        return value
    }
}

data class DoubleNode(val value: Double): ExpressionNode {
    override fun evaluate(context: EvaluationContext): Any {
        return value
    }
}

data class StringNode(val value: String): ExpressionNode {
    override fun evaluate(context: EvaluationContext): Any {
        return value
    }
}

data class BooleanNode(val value: Boolean): ExpressionNode {
    override fun evaluate(context: EvaluationContext): Any {
        return value
    }
}

data class IdentifierNode(val value: String): ExpressionNode {
    override fun evaluate(context: EvaluationContext): Any {
        return context.fetchValue(value)
    }
}

data class ContextualNode(val contextNode: ExpressionNode, val expressionNode: ExpressionNode): ExpressionNode {
    override fun evaluate(context: EvaluationContext): Any {
        val contextValue = contextNode.evaluate(context) as? Map<String, Any> ?: emptyMap()

        return expressionNode.evaluate(context.createSubContext().apply {
            contextValue.entries.map {
                assign(it.key, it.value)
            }
        })
    }
}

data class ListNode(val values: List<ExpressionNode>): ExpressionNode {
    override fun evaluate(context: EvaluationContext): Any {
        return buildList {
            values.forEach {
                add(it.evaluate(context))
            }
        }
    }
}

data class FunctionNode(val parameters: List<String>, val value: Node): ExpressionNode {
    override fun evaluate(context: EvaluationContext): Any {
        return InvokableFunction(context, parameters, ExpressiveNode(value))
    }
}

class ExpressiveNode(val node: Node): ExpressionNode {
    override fun evaluate(context: EvaluationContext): Any {
        if (node is SourceNode) {
            val value = node.nodes.map { if (it is ExpressionNode) it.evaluate(context) else  it.execute(context) }.lastOrNull() ?: return Unit
            return value
        } else if (node is ExpressionNode) {
            return node.evaluate(context)
        }
        return Unit
    }

}