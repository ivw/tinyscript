package tinyscript.compiler.ast

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

object BlockExpressionSpec : Spek({
	describe("block expression") {
		it("returns the final expression type") {
			assertAnalysis("""
				total: Int = (
					# n = 0
					n = n + 2
					n = n + 3
					n
				)
			""")
		}
	}
})
