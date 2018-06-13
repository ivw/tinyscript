package tinyscript.compiler.ast

import org.antlr.v4.runtime.CharStreams
import tinyscript.compiler.parser.parseFile
import tinyscript.compiler.stdlib.StandardLibrary
import kotlin.reflect.KClass
import kotlin.test.assertFailsWith

fun assertAnalysis(codeString: String) {
	val fileCtx = parseFile(CharStreams.fromString(codeString))

	fileCtx.declaration().analyse(StandardLibrary.scope)
}

fun assertAnalysisFails(
	exceptionClass: KClass<out Throwable>,
	codeString: String
) {
	assertFailsWith(exceptionClass) { assertAnalysis(codeString) }
}
