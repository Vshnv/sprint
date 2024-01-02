package io.github.vshnv.sprint.parser.result

import io.github.vshnv.sprint.parser.ast.Node

data class ParseResult(val node: Node?, val errors: List<ParseError>)