package tinyscript.compiler.ast

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

object DeclarationSpec : Spek({
	describe("concrete declaration") {
		it("can have implicit type") {
			assertAnalysis("""
				myString = "foo"
			""")
		}

		it("can have explicit type") {
			assertAnalysis("""
				myString: String = "foo"
			""")
			assertAnalysisFails("""
				myString: Int = "foo"
			""")
		}
	}

	describe("inherit declaration") {
		it("can not be used in local scope") {
			assertAnalysisFails("""
				Point = class [ x: Int = 0, y: Int = 0 ]
				&Point
			""")
		}
	}
})
