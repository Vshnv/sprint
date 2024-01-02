package io.github.vshnv.sprint.parser

import io.github.vshnv.sprint.lexer.Token
import io.github.vshnv.sprint.parser.ast.Node

interface ClauseParser {
    fun parse(list: List<Token>): Node
}