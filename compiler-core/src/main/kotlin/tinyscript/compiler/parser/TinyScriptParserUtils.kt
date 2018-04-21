package tinyscript.compiler.parser

import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream

fun parseFile(input: CharStream): TinyScriptParser.FileContext {
	val lexer = TinyScriptLexer(input)
	val parser = TinyScriptParser(CommonTokenStream(lexer))
	val fileCtx =  parser.file()

	if (parser.numberOfSyntaxErrors > 0)
		throw RuntimeException("parsing failed")

	return fileCtx
}
