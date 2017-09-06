package tinyscript.compiler.core

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

object FunctionExpressionSpec : Spek({
	describe("function expression") {
		it("can be used with and without parameters") {
			assertAnalysis("""
				multiplyByTwo = [n: Int] -> n * 2
				double = multiplyByTwo[n = 1]

				sayHi = -> println[m = "Hi"]
				sayHi[]
			""")
		}

		it("has to be called with the right arguments") {
			assertAnalysisFails("""
				multiplyByTwo = [n: Int] -> n * 2
				foo = multiplyByTwo[]
			""")
		}

		it("must be analysed even when not called") {
			assertAnalysisFails("""
				foo = -> abc
			""")

			assertAnalysisFails("""
				foo = -> (-> abc)
			""")
		}

		it("can be used with an explicit type") {
			assertAnalysis("""
				multiplyByTwo: [n: Int] -> Int = [n: Int] -> n * 2
			""")
			assertAnalysis("""
				multiplyByTwo: [n: Int, foo: String] -> ? = [n: Int] -> n * 2
			""")
			assertAnalysisFails("""
				multiplyByTwo: [] -> Int = [n: Int] -> n * 2
			""")
			assertAnalysisFails("""
				multiplyByTwo: [n: Int] -> String = [n: Int] -> n * 2
			""")
		}

		it("can use forward references until it is called") {
			assertAnalysis("""
				sayHi = -> println[m = hiMessage]
				hiMessage = "Hi"
				sayHi[]
			""")
			assertAnalysisFails("""
				sayHi = -> println[m = hiMessage]
				sayHi[]
				hiMessage = "Hi"
			""")
		}
	}
})
