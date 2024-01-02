package io.github.vshnv.sprint.parser

import io.github.vshnv.sprint.lexer.Token
import java.lang.Integer.min

class TokenTrack(val tokens: List<Token>) {

    init {
        if (tokens.isEmpty()) {
            throw IllegalArgumentException("TokenTrack cannot be created with an empty token list!")
        }
    }

    var cursorIndex: Int = 0
        private set
    val currentToken: Token?
        get() = tokens.getOrNull(cursorIndex)


    fun moveBy(steps: Int = 1): Token? {
        val newIndex = min((cursorIndex + steps).coerceAtLeast(0), tokens.size)
        cursorIndex = newIndex
        return currentToken
    }

    fun moveTo(index: Int): Token? {
        val newIndex = min((index).coerceAtLeast(0), tokens.size)
        cursorIndex = newIndex
        return currentToken
    }

    fun peek(steps: Int = 1): Token? {
        val newIndex = cursorIndex + steps
        return tokens.getOrNull(newIndex)
    }

}