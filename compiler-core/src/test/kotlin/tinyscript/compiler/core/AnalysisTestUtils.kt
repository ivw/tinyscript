package tinyscript.compiler.core

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import tinyscript.compiler.core.parser.TinyScriptLexer
import tinyscript.compiler.core.parser.TinyScriptParser
import tinyscript.compiler.scope.Scope
import tinyscript.compiler.scope.builtInEntities
import kotlin.test.assertFailsWith

fun assertAnalysis(codeString: String) {
	val lexer = TinyScriptLexer(CharStreams.fromString(codeString.trimIndent()))
	val parser = TinyScriptParser(CommonTokenStream(lexer))
	val fileCtx = parser.file()

	if (parser.numberOfSyntaxErrors > 0)
		throw RuntimeException("parsing failed")

	val statementList = fileCtx.statementList().analyse(Scope(null, builtInEntities))
	if (statementList.hasImpureImperativeStatement)
		throw AnalysisException("file scope can not have impure imperative statements")
}

fun assertAnalysisFails(
	codeString: String,
	exceptionClass: kotlin.reflect.KClass<out Throwable> = AnalysisException::class
) {
	assertFailsWith(exceptionClass) { assertAnalysis(codeString) }
}
