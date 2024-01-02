package io.github.vshnv.sprint.parser

class ImmutableEvaluationContext(private val evaluationContext: EvaluationContext? = null): EvaluationContext {
    private val values = mutableMapOf<String, Any>()
    override fun assign(name: String, value: Any) {
        if (values.containsKey(name)) {
            return
        }
        values.put(name, value)
    }

    override fun fetchValue(name: String): Any {
        return values.get(name) ?: evaluationContext?.fetchValue(name) ?: throw RuntimeException("Undefined value requested: ${name}!")
    }

    override fun isAssigned(name: String): Boolean {
        return values.containsKey(name) || (evaluationContext?.isAssigned(name) ?: false)
    }

    override fun createSubContext(): EvaluationContext {
        return ImmutableEvaluationContext(this.solidified())
    }

    private fun solidified(): EvaluationContext {
        val data = values.toMap()
        return object : EvaluationContext {
            override fun assign(name: String, value: Any) {

            }

            override fun fetchValue(name: String): Any {
                return data[name] ?: evaluationContext?.fetchValue(name) ?: throw RuntimeException("Undefined value requested: ${name}|")
            }

            override fun isAssigned(name: String): Boolean {
                return data.containsKey(name) || (evaluationContext?.isAssigned(name) ?: false)
            }

            override fun createSubContext(): EvaluationContext {
                return ImmutableEvaluationContext(this)
            }

        }
    }
}