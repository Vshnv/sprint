package io.github.vshnv.sprint.lexer

class RegexTokenizer: Tokenizer {
    override fun tokenize(input: String): List<Token> {
        val matches = TokenType.MATCHING_REGEX.findAll(input)
        return matches.mapNotNull { result ->
            for (tokenType in TokenType.entries) {
                val matchGroup = result.groups[tokenType.name]
                if (matchGroup != null) {
                    return@mapNotNull Token(tokenType, matchGroup.value, CodePosition(0, matchGroup.range.first))
                }
            }
            null
        }.toList()
    }
}