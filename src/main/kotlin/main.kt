package com.github.wadoon.antlr4doc


import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import org.antlr.parser.antlr4.ANTLRv4Lexer
import org.antlr.parser.antlr4.ANTLRv4Parser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import java.io.File

class Antlr4Doc : CliktCommand("Documentation Generator for ANTLRv4", name = "antlr4-doc") {
    val skipSimpleTokens by option(
        "--skip-simple-tokens",
        help = "Simple tokens, tokens which are only consisting out a list of alternatives literal, will be skipped. Default: skip"
    ).flag("--dont-skip-simple-tokens")

    val title by option("--html-title", help = "title of the documentation")
        .default("Grammar Documentation")

    val files by argument("FILES", help = "list of ANTLR files to render")
        .file().multiple(true)

    val output by option("--output", "-o", help = "HTML output file. Default: output.html")
        .file().default(File("output.html"))

    val completeHtml by option(
        "--complete-html",
        help = "Generate a complete HTML file incl. head and body tags."
    ).flag()

    val cssPath by option(
        "--css",
        help = "CSS files to refer to. Use with `--complete-html`"
    ).multiple(listOf("antlr4-default.css"))

    val sortLexical by option("--sort-lexical", help = "Sort the rules in lexcial order")
        .flag(default = false)

    val out by lazy { output.bufferedWriter() }
    val markdownParser = Parser.builder().build()
    val htmlRenderer = HtmlRenderer.builder().build()
    var tokenMap: Map<String, String> = hashMapOf()

    override fun run() {
        println("antlr4-doc -- Documentation Generator for ANTLRv4")
        println("Version ${Tool.VERSION} GPLv3 https://github.com/wadoon/antlr4-doc/")

        val inputFiles = files.map(::parse)
        val errorFiles = inputFiles
            .mapIndexed { i, a -> if (a == null) i else -1 }
            .filter { it >= 0 }
            .map { files[it] }

        if (errorFiles.isNotEmpty()) {
            error("ABORT. Syntax error in antlr file: $errorFiles")
        }

        if (completeHtml) printHeader()
        val ctxs = inputFiles.filterNotNull()
        val (tokenSpecs, ruleSpecs) = FindRuleSpecs.find(ctxs)
        if (sortLexical) {
            println("sort lex")
            ruleSpecs.sortBy { it.RULE_REF().text }
            tokenSpecs.sortBy { it.TOKEN_REF().text }
        }
        tokenMap = tokenSpecs.mapNotNull { tokenValue(it) }.toMap()
        ctxs.flatMap { it.DOC_COMMENT() }.forEach { writeComment(it.text); }
        tokenSpecs.forEach(::writeTokenSpec)
        ruleSpecs.forEach(::writeRuleSpec)
        if (completeHtml) printFooter()
        out.close()
    }

    private fun writeComment(it: String) {
        val document = markdownParser.parse(it.trim(' ', '\n', '*', '/'))
        val html = htmlRenderer.render(document)
        out.write("<div class=\"comment\">$html</div>")
    }

    private fun writeTokenSpec(it: ANTLRv4Parser.LexerRuleSpecContext) {
        if (it.simpleTokenValue() != null && skipSimpleTokens) return
        it.DOC_COMMENT().forEach { writeComment(it.text); }
        out.write(it.asHtml(tokenMap))
    }

    private fun writeRuleSpec(it: ANTLRv4Parser.ParserRuleSpecContext) {
        it.DOC_COMMENT().forEach { writeComment(it.text); }
        out.write(it.asHtml(tokenMap))
    }

    private fun printHeader() {
        val css = cssPath.joinToString { """<link href="${it}" rel="stylesheet" type="text/css" />""" }
        out.write(
            """
            <html>
            <head>
                <title>$title</title> 
                $css
            </head>
            <body>
        """.trimIndent()
        )

    }

    private fun printFooter() {
        +"</body></html>"
    }


    fun parse(file: File): ANTLRv4Parser.GrammarSpecContext? {
        val lexer = ANTLRv4Lexer(CharStreams.fromFileName(file.absolutePath))
        val parser = ANTLRv4Parser(CommonTokenStream(lexer))
        val gs = parser.grammarSpec()
        if (parser.numberOfSyntaxErrors != 0) return null
        return gs
    }


    private operator fun String.unaryPlus() {
        out.write(this)
        out.write("\n")
    }
}

object Tool {
    val VERSION = "0.1.0"
    @JvmStatic
    fun main(args: Array<String>) = Antlr4Doc().main(args)
}
