package tinyscript.compiler.core

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

object ObjectExpressionSpec : Spek({
	describe("object expression") {
		it("can be used by referencing fields") {
			assertAnalysis("""
				myObject = [ foo = "bar" ]
				myObject.foo
			""")
		}

		it("can be used with an explicit type") {
			assertAnalysis("""
				myObject: [ foo: String ] = [ foo = "bar" ]
				myObject.foo
			""")
			assertAnalysisFails("""
				myObject: [ foo: String ]  = [ abc = 123 ]
				myObject.foo
			""")
			assertAnalysisFails("""
				myObject: [ foo: String ]  = [ foo = "bar", abc = 123 ]
				myObject.abc
			""")
		}
	}
})
