package io.github.vshnv.sprint.parser.result

import io.github.vshnv.sprint.lexer.CodePosition

data class ParseError(val message: String, val position: CodePosition, val length: Int)
