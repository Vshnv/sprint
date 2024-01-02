package io.github.vshnv.sprint.lexer

data class Token(
    val type: TokenType,
    val text: String,
    val codePosition: CodePosition
)