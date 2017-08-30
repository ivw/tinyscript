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
	val lexer = TinyScriptLexer(CharStreams.fromString(codeString.trimIndent()))
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
			""")
		}

		it("allows definition with explicit type") {
			analyse("""
				myString: String = "foo"
			""")
		}

		it("disallows definition with wrong explicit type") {
			assertFails {
				analyse("""
					myString: Int = "foo"
				""")
			}
		}

		it("allows mutable declaration reassign") {
			analyse("""
				# myString = "foo"
				myString <- "bar"
			""")
		}

		it("disallows immutable declaration reassign") {
			assertFails {
				analyse("""
					myString = "foo"
					myString <- "bar"
				""")
			}
		}

		it("allows valid reassignment of definition with implicit nullable type") {
			analyse("""
				# myStringOrNull = <String>?
				myStringOrNull <- "foo"
			""")
		}

		it("disallows invalid reassignment of definition with implicit nullable type") {
			assertFails {
				analyse("""
					# myStringOrNull = <String>?
					myStringOrNull <- 123
				""")
			}
		}

		it("allows valid null-assignment of definition with explicit nullable type") {
			analyse("""
				# myStringOrNull: String? = "foo"
				myStringOrNull <- <String>?
			""")
		}

		it("disallows invalid null-assignment of definition with non-nullable type") {
			assertFails {
				analyse("""
					# myStringOrNull = "foo"
					myStringOrNull <- <String>?
				""")
			}
		}
	}
})
