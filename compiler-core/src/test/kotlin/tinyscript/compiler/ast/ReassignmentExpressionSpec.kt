package tinyscript.compiler.ast

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

object ReassignmentExpressionSpec : Spek({
	describe("reassignment expression") {
		it("can reassign mutable local fields") {
			assertAnalysis("""
				# myString = "foo"
				myString <- "bar"
			""")
			assertAnalysisFails("""
				myString = "foo"
				myString <- "bar"
			""")
		}

		it("can reassign mutable object fields") {
			assertAnalysis("""
				myObject = [ # foo: String? = <String>? ]
				myObject.foo <- "bar"
			""")
			assertAnalysisFails("""
				myObject = [ foo: String? = <String>? ]
				myObject.foo <- "bar"
			""")
		}
	}
})
