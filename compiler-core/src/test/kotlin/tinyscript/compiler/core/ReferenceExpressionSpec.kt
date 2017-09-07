package tinyscript.compiler.core

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

object ReferenceExpressionSpec : Spek({
	describe("reference expression") {
		it("can refer to fields") {
			assertAnalysis("""
				foo = 123
				fooCopy: Int = foo
			""")
			assertAnalysisFails("""
				foo = 123
				fooCopy: Int = abc
			""")
		}

		it("can not refer to fields to a nullable object directly") {
			assertAnalysisFails("""
				myObject: [ foo: String ]?  = [ foo = "bar" ]
				myObject.foo
			""")
		}

		it("can not forward reference in local scope") {
			assertAnalysisFails("""
				myField = forwardField

				forwardField = "foo"
			""")
		}
	}
})
