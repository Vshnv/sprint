package io.github.vshnv.sprint.lexer

interface Tokenizer {
    fun tokenize(input: String): List<Token>
}