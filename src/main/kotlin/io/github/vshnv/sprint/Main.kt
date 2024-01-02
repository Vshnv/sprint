import io.github.vshnv.sprint.lexer.RegexTokenizer
import io.github.vshnv.sprint.parser.EvaluationContext
import io.github.vshnv.sprint.parser.ImmutableEvaluationContext
import io.github.vshnv.sprint.parser.RecDescentClauseParser
import io.github.vshnv.sprint.parser.TokenTrack
import io.github.vshnv.sprint.parser.ast.*
import java.util.Scanner


fun main(args: Array<String>) {
    val tokenizer = RegexTokenizer()
    val parser = RecDescentClauseParser()
    val ec = generateRootContext()
    val input = object {}.javaClass.getResource("/main.sp")?.readText() ?: ""
    val tokens = measureRuntime("Tokenizing") {
        tokenizer.tokenize(input)
    }
    val syntaxTree = measureRuntime("Parsing") {
        parser.parse(tokens)
    }

    measureRuntime("Execution") {
        syntaxTree.execute(ec)
    }


}

fun generateRootContext() = ImmutableEvaluationContext().apply {
    assign("input", InvokableFunction(null, listOf(), native {
        return@native readLine()?.trim() ?: ""
    }))
    assign("print", InvokableFunction(null, listOf("value"), native {
        println(it.fetchValue("value"))
    }))

    assign("listGet", InvokableFunction(null, listOf("list", "idx"), native {
        val list = it.fetchValue("list") as List<*>
        val idx = it.fetchValue("idx") as Int
        return@native list.get(idx) ?: Unit
    }))

    assign("last", InvokableFunction(null, listOf("value"), native {
        val value = it.fetchValue("value")
        if (value is List<*>) {
            return@native value.lastOrNull() ?: Unit
        }
        throw RuntimeException("last can only be called on list")
    }))

    assign("length", InvokableFunction(null, listOf("value"), native {
        val value = it.fetchValue("value")
        if (value is String) {
            return@native value.length
        } else if (value is List<*>) {
            return@native value.size
        }
        throw RuntimeException("Invalid item to call length on")
    }))

    assign("split", InvokableFunction(null, listOf("separator", "input"), native {
        val input = it.fetchValue("input").toString()
        val separator = it.fetchValue("separator").toString()
        return@native input.split(separator)
    }))

    assign("map", InvokableFunction(null, listOf("functor", "col"), native { ec ->
        val col = ec.fetchValue("col")
        val functor = ec.fetchValue("functor") as Invokable
        val operand = col as? List<Any> ?: listOf(col) as? List<Any> ?: emptyList()
        return@native operand.map { functor.invoke(ec, listOf(it)) }
    }))

    assign("filter", InvokableFunction(null, listOf("functor", "col"), native { ec ->
        val col = ec.fetchValue("col")
        val functor = ec.fetchValue("functor") as Invokable
        val operand = col as? List<Any> ?: listOf(col) as? List<Any> ?: emptyList()
        return@native operand.filter {
            val value = functor.invoke(ec, listOf(it))
            if (value is Boolean) return@filter value
            return@filter value != Unit
        }
    }))
    assign("@", object : Invokable {
        override fun invoke(evaluationContext: EvaluationContext, args: List<Any>): Any {
            val lhs = (args[0] as ExpressionNode).evaluate(evaluationContext)
            val rhs = (args[1] as ExpressionNode).evaluate(evaluationContext)
            if (rhs is Invokable) {
                return rhs.invoke(evaluationContext, listOf(lhs))
            }
            throw RuntimeException("Operation not suppourted -- ${lhs} @! ${rhs}")
        }
    })

    assign("<", object : Invokable {
        override fun invoke(evaluationContext: EvaluationContext, args: List<Any>): Any {
            val lhs = (args[0] as ExpressionNode).evaluate(evaluationContext)
            val rhs = (args[1] as ExpressionNode).evaluate(evaluationContext)
            if (lhs is Number && rhs is Number) {
                return lhs.toDouble() < rhs.toDouble()
            }
            throw RuntimeException("Operation not suppourted -- ${lhs} @! ${rhs}")
        }
    })
    assign(">", object : Invokable {
        override fun invoke(evaluationContext: EvaluationContext, args: List<Any>): Any {
            val lhs = (args[0] as ExpressionNode).evaluate(evaluationContext)
            val rhs = (args[1] as ExpressionNode).evaluate(evaluationContext)
            if (lhs is Number && rhs is Number) {
                return lhs.toDouble() > rhs.toDouble()
            }
            throw RuntimeException("Operation not suppourted -- ${lhs} @! ${rhs}")
        }
    })
    assign("+", object : Invokable {
        override fun invoke(evaluationContext: EvaluationContext, args: List<Any>): Any {
            val lhs = (args[0] as ExpressionNode).evaluate(evaluationContext)
            val rhs = (args[1] as ExpressionNode).evaluate(evaluationContext)
            if (lhs is Int && rhs is Int) {
                return lhs + rhs
            } else if (lhs is Number && rhs is Number) {
                return lhs.toDouble() + rhs.toDouble()
            } else if (lhs is String) {
                return lhs + rhs.toString()
            } else if (lhs is List<*> && rhs is List<*>) {
                return lhs + rhs
            } else if (lhs is List<*>) {
                return lhs + rhs
            } else if (rhs is List<*>) {
                return listOf(lhs) + rhs
            }
            throw RuntimeException("Operation not suppourted -- ${lhs} + ${rhs}")
        }
    })
    assign("-", object : Invokable {
        override fun invoke(evaluationContext: EvaluationContext, args: List<Any>): Any {
            val lhs = (args[0] as ExpressionNode).evaluate(evaluationContext)
            val rhs = (args[1] as ExpressionNode).evaluate(evaluationContext)
            if (lhs is Int && rhs is Int) {
                return lhs - rhs
            } else if (lhs is Number && rhs is Number) {
                return lhs.toDouble() - rhs.toDouble()
            } else if (lhs is List<*> && rhs is List<*>) {
                return lhs - rhs
            } else if (lhs is List<*>) {
                return lhs - rhs
            } else if (rhs is List<*>) {
                return listOf(lhs) - rhs
            }
            throw RuntimeException("Operation not suppourted -- ${lhs} + ${rhs}")
        }
    })
    assign("*", object : Invokable {
        override fun invoke(evaluationContext: EvaluationContext, args: List<Any>): Any {
            val lhs = (args[0] as ExpressionNode).evaluate(evaluationContext)
            val rhs = (args[1] as ExpressionNode).evaluate(evaluationContext)
            if (lhs is Int && rhs is Int) {
                return lhs * rhs
            } else if (lhs is Number && rhs is Number) {
                return lhs.toDouble() * rhs.toDouble()
            }
            throw RuntimeException("Operation not suppourted -- ${lhs} + ${rhs}")
        }
    })
    assign("==", object : Invokable {
        override fun invoke(evaluationContext: EvaluationContext, args: List<Any>): Any {
            val lhs = (args[0] as ExpressionNode).evaluate(evaluationContext)
            val rhs = (args[1] as ExpressionNode).evaluate(evaluationContext)
            return lhs == rhs
        }
    })
    assign("!=", object : Invokable {
        override fun invoke(evaluationContext: EvaluationContext, args: List<Any>): Any {
            val lhs = (args[0] as ExpressionNode).evaluate(evaluationContext)
            val rhs = (args[1] as ExpressionNode).evaluate(evaluationContext)
            return lhs != rhs
        }
    })
}

//defaultFirehoseEventDispatcher().dispatch(event: SaraEvent(tId: t_id, pId: p_id, action: SaraAction.Click, clickOrigin: "Search", assetId: assetId, platform: "iOS", userId: userId))

private inline fun <T> measureRuntime(name: String, func: () -> T): T {
    val start = System.currentTimeMillis()
    val res = func()
    val end = System.currentTimeMillis()
    println("$name took ${end-start}ms")
    return res
}

fun fetchBasicMemory(): MutableMap<String, Any> {
    return buildMap {
        put("print", InvokableFunction(null, listOf("value"), native {
            println(it.fetchValue("value"))
        }))
        put("map", InvokableFunction(null, listOf("functor", "col"), native { ec ->
            val col = ec.fetchValue("col")
            val functor = ec.fetchValue("functor") as Invokable
            val operand = col as? List<Any> ?: listOf(col) as? List<Any> ?: emptyList()
            return@native operand.map { functor.invoke(ec, operand) }
        }))
        put("+", object : Invokable {
            override fun invoke(evaluationContext: EvaluationContext, args: List<Any>): Any {
                val lhs = (args[0] as ExpressionNode).evaluate(evaluationContext)
                val rhs = (args[1] as ExpressionNode).evaluate(evaluationContext)
                if (lhs is Int && rhs is Int) {
                    return lhs + rhs
                } else if (lhs is Number && rhs is Number) {
                    return lhs.toDouble() + rhs.toDouble()
                } else if (lhs is String) {
                    return lhs + rhs.toString()
                }
                throw RuntimeException("Operation not suppourted -- ${lhs} + ${rhs}")
            }
        })
        put("*", object : Invokable {
            override fun invoke(evaluationContext: EvaluationContext, args: List<Any>): Any {
                val lhs = (args[0] as ExpressionNode).evaluate(evaluationContext)
                val rhs = (args[1] as ExpressionNode).evaluate(evaluationContext)
                if (lhs is Int && rhs is Int) {
                    return lhs * rhs
                } else if (lhs is Number && rhs is Number) {
                    return lhs.toDouble() * rhs.toDouble()
                }
                throw RuntimeException("Operation not suppourted -- ${lhs} + ${rhs}")
            }
        })

//        put("let", object : Invokable {
//            override fun invoke(memory: MutableMap<String, Any>, args: List<Expression>): Any {
//                for (i in (args.indices step 2)) {
//                    val id = args.get(i) as Identifier
//                    val value = args.get(i + 1).evaluate(memory)
//                    if (memory.containsKey(id.name)) {
//                        throw RuntimeException("Variable redeclaration not allowed")
//                    }
//                    memory[id.name] = value
//                }
//                return Unit
//            }
//        })
//        put("add", object : Invokable {
//            override fun invoke(memory: MutableMap<String, Any>, args: List<Expression>): Any {
//                return args.map { it.evaluate(memory) }.reduce( { a, b ->
//                    (a as Int) + (b as Int)
//                })
//            }
//        })
//        put("fn", object : Invokable {
//            override fun invoke(memory: MutableMap<String, Any>, args: List<Expression>): Any {
//                for (i in (0..args.size step 2)) {
//                    val fnArgs = args.get(0) as DataList
//                    if (!fnArgs.elements.all { it is Identifier }) {
//                        throw RuntimeException("fn args have to be identifiers")
//                    }
//                    val value = args.get(1)
//                    return object : Invokable {
//                        override fun invoke(memory: MutableMap<String, Any>, args: List<Expression>): Any {
//                            val scopedMemory = buildMap {
//                                putAll(memory)
//                                fnArgs.elements.forEachIndexed { idx, fnArg ->
//                                    val id = (fnArg as Identifier).name
//                                    val value = args.get(idx).evaluate(memory)
//                                    put(id, value)
//                                }
//                            }.toMutableMap()
//                            return value.evaluate(scopedMemory)
//                        }
//                    }
//
//                }
//                return Unit
//            }
//        })
    }.toMutableMap()
}