package io.github.vshnv.sprint.parser

import io.github.vshnv.sprint.lexer.CodePosition
import io.github.vshnv.sprint.lexer.Token
import io.github.vshnv.sprint.lexer.TokenType
import io.github.vshnv.sprint.parser.ast.*
import io.github.vshnv.sprint.parser.result.ParseError
import io.github.vshnv.sprint.parser.result.ParseException
import io.github.vshnv.sprint.parser.result.ParseResult

class RecDescentClauseParser: ClauseParser {
    override fun parse(list: List<Token>): Node {
        return ClauseRecDescentGrammar(TokenTrack(list)).source()
    }
}

private class ClauseRecDescentGrammar(private val tokenTrack: TokenTrack) {
    private val unmatchedGrammarResult = ParseResult(null, listOf(ParseError("Unmatched Grammar", CodePosition(0, 0), 0)))


    inline fun parseAny(vararg parseOptions: () -> Node?): Node? {
        for (parse in parseOptions) {
            val result = parse()
            if (result != null) {
                return result
            }
        }
        return null
    }

    private fun spaces() {
        while (matchCurrentType(TokenType.WHITESPACE)) {}
    }

    private fun spacesAndLines() {
        while (matchCurrentType(TokenType.WHITESPACE, TokenType.NEWLINE)) {}

    }

    fun matchCurrentType(vararg tokens: TokenType): Boolean {
        val currentTokenType = tokenTrack.currentToken
        return (currentTokenType?.type in tokens).also {
            if (it) {
                tokenTrack.moveBy(1)
            }
        }
    }

    fun matchCurrentType(tokenType: TokenType, text: String) = matchCurrentType(tokenType, listOf(text))

    fun matchCurrentType(tokenType: TokenType, texts: List<String>? = null) = (tokenTrack.currentToken?.type == tokenType && (texts == null || texts.contains(tokenTrack.currentToken?.text))).also {
        if (it) {
            tokenTrack.moveBy(1)
        }
    }

    fun <T: Node> required(node: T?): T {
        if (node == null) {
            throw ParseException("Failed required node")
        }
        return node
    }

    fun expect(tokenType: TokenType): Token {
        val token = tokenTrack.currentToken
        if (!matchCurrentType(tokenType)) {
            throw ParseException("Expected ${tokenType}")
        }
        return token!!
    }

    fun source(): Node {
        val sourceList = mutableListOf<Node>()
        while (tokenTrack.cursorIndex < tokenTrack.tokens.size) {
            matchCurrentType(TokenType.NEWLINE, TokenType.WHITESPACE)
            val element = root()
            sourceList.add(element)
        }
        return SourceNode(sourceList)
    }


    fun root(): Node {
        return parseAny(::variableDefinition, ::expression) ?: throw ParseException("Invalid root parse")
    }

    fun variableDefinition(): Node? {
        if (matchCurrentType(TokenType.LET)) {
            expect(TokenType.WHITESPACE)
            val identifier = required(identifier())
            spaces()
            expect(TokenType.ASSIGN)
            spacesAndLines()
            val expression = expression()
            return DefineNode(identifier.value, expression)
        }
        return null
    }


    fun expression(): ExpressionNode {
        return assignment()
    }

    private fun assignment(): ExpressionNode {
        val lhs = or()
        spaces()
        if (matchCurrentType(TokenType.ASSIGN)) {
            spacesAndLines()
            val rhs = or()
            if (lhs is IdentifierNode) {
                return AssignNode(null, lhs.value, rhs)
            } else if (lhs is ContextualNode && lhs.expressionNode is IdentifierNode) {
                return AssignNode(lhs.contextNode, lhs.expressionNode.value, rhs)
            }
            throw ParseException("Invalid node at Assignment operation")
        }
        return lhs
    }

    private fun or(): ExpressionNode {
        var lhs = and()
        spaces()
        while (matchCurrentType(TokenType.OPERATOR, "||")) {
            spacesAndLines()
            val rhs = and()
            lhs = BinaryOperationNode(lhs, "||", rhs)
            spaces()
        }
        return lhs
    }

    private fun and(): ExpressionNode {
        var lhs = equality()
        spaces()
        while (matchCurrentType(TokenType.OPERATOR, "&&")) {
            spacesAndLines()
            val rhs = equality()
            lhs = BinaryOperationNode(lhs, "&&", rhs)
            spaces()
        }
        return lhs
    }

    private fun equality(): ExpressionNode {
        var lhs = comparision()
        spaces()
        while (matchCurrentType(TokenType.OPERATOR, listOf("==", "!="))) {
            tokenTrack.moveBy(-1)
            val operator = expect(TokenType.OPERATOR)
            spacesAndLines()
            val rhs = comparision()
            lhs = BinaryOperationNode(lhs, operator.text, rhs)
            spaces()
        }
        return lhs
    }

    private fun comparision(): ExpressionNode {
        var lhs = additionSubtraction()
        spaces()
        while (matchCurrentType(TokenType.OPERATOR, listOf(">=", "<=", "<", ">"))) {
            tokenTrack.moveBy(-1)
            val operator = expect(TokenType.OPERATOR)
            spacesAndLines()
            val rhs = additionSubtraction()
            lhs = BinaryOperationNode(lhs, operator.text, rhs)
            spaces()
        }
        return lhs
    }

    private fun additionSubtraction(): ExpressionNode {
        var lhs = multiplicationDivision()
        spaces()
        while (matchCurrentType(TokenType.OPERATOR, listOf("+", "-"))) {
            tokenTrack.moveBy(-1)
            val operator = expect(TokenType.OPERATOR)
            spaces()
            val rhs = multiplicationDivision()
            lhs = BinaryOperationNode(lhs, operator.text, rhs)
            spaces()
        }
        return lhs
    }

    private fun multiplicationDivision(): ExpressionNode {
        var lhs = unary()
        spaces()
        while (matchCurrentType(TokenType.OPERATOR, listOf("*", "/"))) {
            tokenTrack.moveBy(-1)
            val operator = expect(TokenType.OPERATOR)
            spaces()
            val rhs = unary()
            lhs = BinaryOperationNode(lhs, operator.text, rhs)
            spaces()
        }
        return lhs
    }

    private fun unary(): ExpressionNode {
        if (matchCurrentType(TokenType.OPERATOR, listOf("-", "!"))) {
            tokenTrack.moveBy(-1)
            val operator = expect(TokenType.OPERATOR)
            val operand = unary()
            return UnaryOperationNode(operator.text, operand)
        }
        return functionApplication()
    }

    private fun functionApplication(): ExpressionNode {
        var lhs = contextual()
        spaces()
        while (matchCurrentType(TokenType.OPERATOR, listOf("@"))) {
            tokenTrack.moveBy(-1)
            val operator = expect(TokenType.OPERATOR)
            spaces()
            val rhs = contextual()
            lhs = BinaryOperationNode(lhs, operator.text, rhs)
            spaces()
        }
        return lhs
    }

    private fun contextual(): ExpressionNode {
        var context = invocation()
        spacesAndLines()
        while (matchCurrentType(TokenType.DOT)) {
            val invocation = invocation()
            spacesAndLines()
            context = ContextualNode(context, invocation)
        }
        return context
    }

    // test.test()
    // invocation . invocation . in

    private fun invocation(): ExpressionNode {
        var operand = bracket()
        while (matchCurrentType(TokenType.LPAR)) {
            val args = mutableListOf<ExpressionNode>()
            spacesAndLines()
            while (!matchCurrentType(TokenType.RPAR)) {
                args.add(expression())
                spaces()
                if (!matchCurrentType(TokenType.COMMA) && tokenTrack.currentToken?.type != TokenType.RPAR) {
                    throw ParseException("Invalid separator in function invocation")
                }
                spacesAndLines()
            }
            operand = InvocationNode(operand, args)
            spaces()
        }
        spaces()
        val position = tokenTrack.cursorIndex
        if (matchCurrentType(TokenType.LBRAC) && spaces() != null && matchCurrentType(TokenType.LPAR)) {
            tokenTrack.moveTo(position)
            val function = fundef()
            operand = InvocationNode(operand, listOf(function))
        } else {
            tokenTrack.moveTo(position)
        }
        return operand
    }

    private fun bracket(): ExpressionNode {
        if (matchCurrentType(TokenType.LPAR)) {
            spacesAndLines()
            val expressionNode = expression()
            spacesAndLines()
            expect(TokenType.RPAR)
            return expressionNode
        }
        return conditional()
    }

    private fun conditional(): ExpressionNode {
        if (matchCurrentType(TokenType.IF)) {
            spaces()
            val condition = expression()
            spaces()
            expect(TokenType.LBRAC)
            spacesAndLines()
            val truthyNodes = mutableListOf<Node>()
            while (!matchCurrentType(TokenType.RBRAC)) {
                truthyNodes.add(root())
                spacesAndLines()
            }
            spacesAndLines()
            if (matchCurrentType(TokenType.ELSE)) {
                spaces()
                if (matchCurrentType(TokenType.IF)) {
                    tokenTrack.moveBy(-1)
                    val elseConditional = conditional()
                    return ConditionalNode(condition, ExpressiveNode(SourceNode(truthyNodes)), elseConditional)
                }
                expect(TokenType.LBRAC)
                spacesAndLines()
                val falsyNodes = mutableListOf<Node>()
                while (!matchCurrentType(TokenType.RBRAC)) {
                    falsyNodes.add(root())
                    spacesAndLines()
                }
                return ConditionalNode(condition, ExpressiveNode(SourceNode(truthyNodes)), ExpressiveNode(SourceNode(falsyNodes)))
            } else {
                return ConditionalNode(condition, ExpressiveNode(SourceNode(truthyNodes)))
            }
        }
        return fundef()
    }

    private fun dataCreate(): ExpressionNode {
        if (matchCurrentType(TokenType.LBRAC)) {
            spacesAndLines()
            val entries = mutableListOf<Pair<String, ExpressionNode>>()
            while (!matchCurrentType(TokenType.RBRAC)) {
                spacesAndLines()
                val key = identifier()!!
                spaces()
                expect(TokenType.COLON)
                spacesAndLines()
                val value = expression()
                entries.add(key.value to value)
                if (!matchCurrentType(TokenType.COMMA) && tokenTrack.currentToken?.type != TokenType.RBRAC) {
                    throw ParseException("Invalid separator in list creation")
                }
                spacesAndLines()
            }
            return DataCreateNode(entries)
        }
        return list()
    }

    private fun fundef(): ExpressionNode {
        val startPosition = tokenTrack.cursorIndex
        if (matchCurrentType(TokenType.LBRAC)) {
            spacesAndLines()
            val argNames = mutableListOf<String>()
            if (matchCurrentType(TokenType.LPAR)) {
                while (!matchCurrentType(TokenType.RPAR)) {
                    argNames.add(identifier()!!.value)
                    spaces()
                    if (!matchCurrentType(TokenType.COMMA) && tokenTrack.currentToken?.type != TokenType.RPAR) {
                        throw ParseException("Invalid separator in list creation")
                    }
                    spacesAndLines()
                }
                spacesAndLines()
                val nodes = mutableListOf<Node>()
                while (!matchCurrentType(TokenType.RBRAC)) {
                    nodes.add(root())
                    spacesAndLines()
                }
                return FunctionNode(argNames, SourceNode(nodes))
            } else {
                tokenTrack.moveTo(startPosition)
                return dataCreate()
            }
        } else {
            return dataCreate()
        }
    }


    private fun list(): ExpressionNode {
        if (matchCurrentType(TokenType.LSQR)) {
            val elements = mutableListOf<ExpressionNode>()
            spacesAndLines()
            while (!matchCurrentType(TokenType.RSQR)) {
                elements.add(expression())
                spaces()
                if (!matchCurrentType(TokenType.COMMA) && tokenTrack.currentToken?.type != TokenType.RSQR) {
                    throw ParseException("Invalid separator in list creation")
                }
                spacesAndLines()
            }
            return ListNode(elements)
        }
        return primary()
    }


    private fun primary(): ExpressionNode {
        return integer() ?: double() ?: boolean() ?: string() ?: identifier() ?: throw ParseException("Invalid primary token")
    }


    private fun integer(): IntNode? {
        if (matchCurrentType(TokenType.INT)) {
            tokenTrack.moveBy(-1)
            val token = expect(TokenType.INT)
            return IntNode(token.text.toInt())
        }
        return null
    }
    
    private fun double(): DoubleNode? {
        if (matchCurrentType(TokenType.DOUBLE)) {
            tokenTrack.moveBy(-1)
            val token = expect(TokenType.DOUBLE)
            return DoubleNode(token.text.toDouble())
        }
        return null
    }

    private fun boolean(): BooleanNode? {
        if (matchCurrentType(TokenType.BOOLEAN)) {
            tokenTrack.moveBy(-1)
            val token = expect(TokenType.BOOLEAN)
            return BooleanNode(token.text.toBoolean())
        }
        return null
    }

    private fun string(): StringNode? {
        if (matchCurrentType(TokenType.STRING)) {
            tokenTrack.moveBy(-1)
            val token = expect(TokenType.STRING)
            return StringNode(token.text.substring(1, token.text.length - 1))
        }
        return null
    }

    fun identifier(): IdentifierNode? {
        if (tokenTrack.currentToken?.type == TokenType.IDENTIFIER) {
            return IdentifierNode(tokenTrack.currentToken!!.text).also {
                tokenTrack.moveBy(1)
            }
        }
        return null
    }
}
