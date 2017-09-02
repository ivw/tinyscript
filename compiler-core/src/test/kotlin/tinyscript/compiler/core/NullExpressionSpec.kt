package tinyscript.compiler.core

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

object NullExpressionSpec : Spek({
	describe("null expression") {
		it("can be used with an explicit type") {
			assertAnalysis("""
				myString: String? = <String>?
			""")
			assertAnalysisFails("""
				myString: String = <String>?
			""")
			assertAnalysisFails("""
				myField: String? = ?
			""")
		}

		it("can be used with implicit type and reassignment") {
			assertAnalysis("""
				# myField = <String>?
				myField <- "foo"
			""")
			assertAnalysisFails("""
				# myField = <String>?
				myField <- 123
			""")
			assertAnalysis("""
				# myField: String? = "foo"
				myField <- <String>?
			""")
			assertAnalysisFails("""
				# myField = "foo"
				myField <- <String>?
			""")
		}

	}
})
