package tinyscript.compiler.core

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import tinyscript.compiler.core.parser.TinyScriptLexer
import tinyscript.compiler.core.parser.TinyScriptParser
import kotlin.test.assertFails

fun assertAnalysis(codeString: String) {
	val lexer = TinyScriptLexer(CharStreams.fromString(codeString.trimIndent()))
	val parser = TinyScriptParser(CommonTokenStream(lexer))
	val fileCtx = parser.file()

	if (parser.numberOfSyntaxErrors > 0)
		throw RuntimeException("parsing failed")

	fileCtx.declarations().declaration().analyse(Scope(null, builtInEntities))
}

fun assertAnalysisFails(codeString: String) {
	assertFails { assertAnalysis(codeString) }
}
