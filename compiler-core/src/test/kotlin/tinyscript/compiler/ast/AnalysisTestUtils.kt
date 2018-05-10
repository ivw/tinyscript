package tinyscript.compiler.ast

import org.antlr.v4.runtime.CharStreams
import tinyscript.compiler.parser.parseFile
import tinyscript.compiler.stdlib.StandardLibrary
import kotlin.reflect.KClass
import kotlin.test.assertFailsWith

fun assertAnalysis(codeString: String, allowImpure: Boolean = false) {
	val fileCtx = parseFile(CharStreams.fromString(codeString))

	val statements = fileCtx.statementList().statement()
	if (allowImpure) {
		statements.analyse(StandardLibrary.scope)
	} else {
		statements.analysePure(StandardLibrary.scope)
	}
}

fun assertAnalysisFails(
	exceptionClass: KClass<out Throwable>,
	codeString: String,
	allowImpure: Boolean = false
) {
	assertFailsWith(exceptionClass) { assertAnalysis(codeString, allowImpure) }
}
