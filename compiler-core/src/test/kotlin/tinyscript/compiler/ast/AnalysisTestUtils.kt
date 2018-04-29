package tinyscript.compiler.ast

import org.antlr.v4.runtime.CharStreams
import tinyscript.compiler.parser.parseFile
import tinyscript.compiler.stdlib.StandardLibrary
import kotlin.test.assertFailsWith

fun assertAnalysis(codeString: String) {
	val fileCtx = parseFile(CharStreams.fromString(codeString))

	fileCtx.statementList().statement().analysePure(StandardLibrary.scope)
}

fun assertAnalysisFails(
	codeString: String,
	exceptionClass: kotlin.reflect.KClass<out Throwable>
) {
	assertFailsWith(exceptionClass) { assertAnalysis(codeString) }
}
