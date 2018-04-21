package tinyscript.compiler.ast

import org.antlr.v4.runtime.CharStreams
import tinyscript.compiler.parser.parseFile
import tinyscript.compiler.stdlib.StandardLibrary
import kotlin.test.assertFailsWith

fun assertAnalysis(codeString: String) {
	val fileCtx = parseFile(CharStreams.fromString(codeString.trimIndent()))

	fileCtx.statementList().statement().analysePure(StandardLibrary.scope)
}

fun assertAnalysisFails(
	codeString: String,
	exceptionClass: kotlin.reflect.KClass<out Throwable> = AnalysisException::class
) {
	assertFailsWith(exceptionClass) { assertAnalysis(codeString) }
}
