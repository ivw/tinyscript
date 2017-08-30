package tinyscript.compiler.core

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import tinyscript.compiler.core.parser.TinyScriptLexer
import tinyscript.compiler.core.parser.TinyScriptParser
import kotlin.test.assertFails

fun analyse(codeString: String) {
	val lexer = TinyScriptLexer(CharStreams.fromString(codeString))
	val parser = TinyScriptParser(CommonTokenStream(lexer))
	val fileCtx = parser.file()

	if (parser.numberOfSyntaxErrors > 0)
		throw RuntimeException("parsing failed")

	val analysisVisitor = AnalysisVisitor("testCode")
	analysisVisitor.visitFile(fileCtx)
	analysisVisitor.finishDeferredAnalyses()
}

object AnalysisSpec : Spek({
	describe("analysis") {
		it("allows definition with implicit type") {
			analyse("""
				myString = "foo"
			""".trimIndent())
		}

		it("allows definition with explicit type") {
			analyse("""
				myString: String = "foo"
			""".trimIndent())
		}

		it("disallows definition with wrong explicit type") {
			assertFails {
				analyse("""
					myString: Int = "foo"
				""".trimIndent())
			}
		}
	}
})
