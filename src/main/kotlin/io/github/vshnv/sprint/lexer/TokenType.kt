package io.github.vshnv.sprint.lexer

enum class TokenType(val regex: String) {
    STRING("\"(?:[^\"\\\\]|\\\\.)*\""),
    DOT("\\."),
    COMMA("\\,"),
    COLON("\\:"),
    WHITESPACE(" +"),

    NEWLINE("\n|[\r\n]"),
    LET("\\blet\\b"),
    IF("if"),
    ELSE("else"),
    OPERATOR("(\\+|-|\\*|/|\\@|>|<|>=|==|!=|<=|&|\\||%|!|\\^)"),
    ASSIGN("="),
    BOOLEAN("true|false"),

    IDENTIFIER("[_a-zA-Z][_a-zA-Z0-9]{0,30}"),
    DOUBLE("\\d+\\.\\d+"),
    INT("\\d+"),
    LPAR("\\("),
    RPAR("\\)"),
    LSQR("\\["),
    RSQR("\\]"),
    LBRAC("\\{"),
    RBRAC("\\}");

    companion object {
        val MATCHING_REGEX = entries.joinToString(separator = "|") {
            "(?<${it.name}>${it.regex})"
        }.toRegex()
    }
}