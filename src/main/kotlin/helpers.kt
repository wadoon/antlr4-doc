package com.github.wadoon.antlr4doc

import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import org.antlr.parser.antlr4.ANTLRv4Lexer
import org.antlr.parser.antlr4.ANTLRv4Parser
import org.antlr.parser.antlr4.ANTLRv4ParserBaseVisitor
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.TerminalNode
import java.io.StringWriter


class FindRuleSpecs : ANTLRv4ParserBaseVisitor<Unit>() {
    val ruleSpecs = arrayListOf<ANTLRv4Parser.ParserRuleSpecContext>()
    override fun visitParserRuleSpec(ctx: ANTLRv4Parser.ParserRuleSpecContext) {
        ruleSpecs.add(ctx)
    }

    val tokenSpecs = arrayListOf<ANTLRv4Parser.LexerRuleSpecContext>()
    override fun visitLexerRuleSpec(ctx: ANTLRv4Parser.LexerRuleSpecContext) {
        tokenSpecs.add(ctx)
    }

    companion object {
        fun find(e: Iterable<ParserRuleContext>): Pair<ArrayList<ANTLRv4Parser.LexerRuleSpecContext>, ArrayList<ANTLRv4Parser.ParserRuleSpecContext>> {
            return FindRuleSpecs().let { frs ->
                e.forEach { it.accept(frs) }
                frs.tokenSpecs to frs.ruleSpecs
            }
        }
    }
}

fun ANTLRv4Parser.LexerRuleSpecContext.simpleTokenValue(): String? {
    val alternatives = lexerRuleBlock().lexerAltList().lexerAlt().map {
        val le = it.lexerElements().lexerElement()
        if (le.size == 1) le.first().lexerAtom()?.terminal()?.STRING_LITERAL()
        else null
    }
    val allStrings = alternatives.all { it != null }
    if (alternatives.isNotEmpty() && allStrings) return alternatives.first()?.text
    return null
}

fun tokenValue(ctx: ANTLRv4Parser.LexerRuleSpecContext): Pair<String, String>? {
    return ctx.simpleTokenValue()?.let { ctx.TOKEN_REF().text to it }
}

fun ANTLRv4Parser.LexerRuleSpecContext.asHtml(tokenMap: Map<String, String>, tokens: List<Token>): String {
    val out = StringWriter()
    out.appendHTML(true)
        .div("rule") {
            a {
                this.attributes["id"] = TOKEN_REF().text
                +TOKEN_REF().text
            }
            +":"
            div("rule-body") {
                accept(htmlVisitor(tokens, tokenMap))
            }
        }
    return out.toString()
}

fun ANTLRv4Parser.ParserRuleSpecContext.asHtml(tokenMap: Map<String, String>, tokens: List<Token>): String {
    val out = StringWriter()
    out.appendHTML(true)
        .div("rule") {
            a {
                this.attributes["id"] = RULE_REF().text
                +RULE_REF().text
            }
            +":"
            div("rule-body") {
                accept(htmlVisitor(tokens, tokenMap))
            }
        }
    return out.toString()
}

private fun DIV.htmlVisitor(tokens: List<Token>, tokenMap: Map<String, String>): ANTLRv4ParserBaseVisitor<Unit> {
    return object : ANTLRv4ParserBaseVisitor<Unit>() {
        val self = this
        fun ParserRuleContext?.accept() = this?.accept(self)

        private fun printWhitespace(start: Token) {
            val pos = tokens.indexOfFirst { it.startIndex == start.startIndex }
            if (pos > 0) {
                var cur = pos - 1
                while (true) {
                    val prev = tokens[cur]
                    if (prev.channel != ANTLRv4Lexer.OFF_CHANNEL || cur <= 0) {
                        break
                    }
                    unsafe {
                        +prev.text.replace(" ", "&nbsp;")
                            .replace("\n", "<br>")
                            .replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;")
                    }
                    cur--
                }
            }
        }

        override fun visitRuleBlock(ctx: ANTLRv4Parser.RuleBlockContext) {
            printWhitespace(ctx.start)
            super.visitRuleBlock(ctx); antlr(";")
        }


        override fun visitRuleAltList(ctx: ANTLRv4Parser.RuleAltListContext) {
            printWhitespace(ctx.start)
            val c = if (ctx.labeledAlt().size == 0) "empty-alternative-list" else "alternative-list"
            span(c) {
                ctx.labeledAlt().forEachIndexed { index, altContext ->
                    span("alternative alternative-$index") {
                        if (index != 0) antlr("|")
                        altContext.accept()
                    }
                }
            }
        }

        override fun visitLabeledAlt(ctx: ANTLRv4Parser.LabeledAltContext) {
            printWhitespace(ctx.start)
            ctx.alternative().accept()
            if (ctx.identifier() != null) {
                span("label") { +ctx.identifier().text }
            }
        }

        override fun visitAlternative(ctx: ANTLRv4Parser.AlternativeContext) {
            printWhitespace(ctx.start)
            ctx.element().forEachIndexed { index, altContext ->
                if (index != 0 && index != ctx.element().size - 1) +" "
                altContext.accept()
            }
        }

        override fun visitLabeledElement(ctx: ANTLRv4Parser.LabeledElementContext) {
            printWhitespace(ctx.start)
            span("labeled-element") {
                ctx.atom().accept()
                ctx.block().accept()
                sub("label") {
                    +ctx.identifier().text
                    //if (null != ctx.ASSIGN()) +"="
                    //if (null != ctx.PLUS_ASSIGN()) +"+="
                }
            }
        }

        override fun visitElementOption(ctx: ANTLRv4Parser.ElementOptionContext) {
            printWhitespace(ctx.start)
            span("element-option") { +ctx.text }
        }

        override fun visitEbnfSuffix(ctx: ANTLRv4Parser.EbnfSuffixContext) {
            printWhitespace(ctx.start)
            span("ebnf-suffx") { +ctx.text }
        }

        override fun visitActionBlock(ctx: ANTLRv4Parser.ActionBlockContext) {
            printWhitespace(ctx.start)
            super.visitActionBlock(ctx)
        }

        //region atom
        override fun visitTerminal(ctx: ANTLRv4Parser.TerminalContext) {
            printWhitespace(ctx.start)
            span("terminal") {
                val tokenRefText = ctx.TOKEN_REF()?.text
                if (tokenRefText != null) {
                    if (tokenRefText in tokenMap) {
                        printStringLiteral(tokenMap[tokenRefText]!!)
                    } else {
                        span("token-ref") {
                            a(href = "#${ctx.TOKEN_REF().text}") { +ctx.TOKEN_REF().text }
                        }
                    }
                }
                if (ctx.STRING_LITERAL() != null) {
                    printStringLiteral(ctx)
                }
                ctx.elementOptions().accept()
            }
        }

        private fun printStringLiteral(ctx: ANTLRv4Parser.TerminalContext) {
            printWhitespace(ctx.start)
            printStringLiteral(ctx.STRING_LITERAL().text)
        }

        private fun printStringLiteral(text: String) {
            span("token-ref string-literal") {
                +text
                    .replace("\\\\", "\\")
                    .trim('\'')
            }
        }

        override fun visitRuleref(ctx: ANTLRv4Parser.RulerefContext) {
            printWhitespace(ctx.start)
            span("rule-ref") {
                printReference(ctx.RULE_REF())
                ctx.argActionBlock().accept()
                ctx.elementOptions().accept()
            }
        }

        private fun printReference(ruleRef: TerminalNode) {
            a(href = "#${ruleRef.text}") { +ruleRef.text }
        }

        override fun visitNotSet(ctx: ANTLRv4Parser.NotSetContext) {
            printWhitespace(ctx.start)
            span("not") {
                span("NOT") { +"^" }
                ctx.setElement().accept()
                ctx.blockSet().accept()
            }
        }

        override fun visitSetElement(ctx: ANTLRv4Parser.SetElementContext) {
            printWhitespace(ctx.start)
            if (ctx.TOKEN_REF() != null) printReference(ctx.TOKEN_REF())
            if (ctx.STRING_LITERAL() != null) printStringLiteral(ctx.STRING_LITERAL().text)
            ctx.characterRange().accept()
            if (ctx.LEXER_CHAR_SET() != null) {
                +ctx.LEXER_CHAR_SET().text
            }
            ctx.elementOptions().accept()
        }


        override fun visitBlockSet(ctx: ANTLRv4Parser.BlockSetContext) {
            printWhitespace(ctx.start)
            antlr("(")
            ctx.setElement().forEachIndexed { index, context ->
                if (index != 0) antlr("|")
                context.accept()
            }
            antlr(")")
        }

        private fun antlr(s: String) = span("meta") { +s }
        //endregion

        override fun visitBlock(ctx: ANTLRv4Parser.BlockContext) {
            printWhitespace(ctx.start)
            antlr("(")
            ctx.optionsSpec().accept()
            ctx.ruleAction().forEach { it.accept() }
            if (ctx.COLON() != null) antlr(":")
            ctx.altList().accept()
            antlr(")")
        }

        override fun visitElementOptions(ctx: ANTLRv4Parser.ElementOptionsContext) {
            printWhitespace(ctx.start)
            antlr("<")
            ctx.elementOption().forEach { it.accept() }
            antlr(">")
        }

        override fun visitLexerAltList(ctx: ANTLRv4Parser.LexerAltListContext) {
            printWhitespace(ctx.start)
            val c = if (ctx.lexerAlt().size <= 1) "" else "alternative-list"
            span(c) {
                ctx.lexerAlt().forEachIndexed { index, altContext ->
                    if (index != 0) {
                        antlr("|")
                    }
                    altContext.accept()
                }
            }
        }
    }
}
