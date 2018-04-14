package tinyscript.compiler.ast

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

object StatementListSpec : Spek({
	describe("ImperativeStatement") {
		it("can state a pure expression in file scope") {
			assertAnalysis("""
				123
			""")
			assertAnalysis("""
				abc = 123
			""")
		}
		it("can not state an impure expression in file scope") {
			assertAnalysisFails("""
				println![]
			""")
			assertAnalysisFails("""
				result = println![]
			""")
		}
		it("can state an impure expression in impure scope") {
			assertAnalysis("""
				main! => (
					println![]
					0
				)
			""")
			assertAnalysis("""
				main! => (
					result = println![]
					0
				)
			""")
		}
	}

	describe("FunctionDeclaration") {
		it("can declare a pure function with a pure expression") {
			assertAnalysis("""
				getOne => 1
			""")
			assertAnalysis("""
				double[n: Int] => n * 2
			""")
		}
		it("can not declare a pure function with an impure expression") {
			assertAnalysisFails("""
				printEmptyLine => println![]
			""")
			assertAnalysisFails("""
				printDouble[n: Int] => println![m = n * 2]
			""")
		}
		it("can declare an impure function with an impure expression") {
			assertAnalysis("""
				printEmptyLine! => println![]
			""")
			assertAnalysis("""
				printDouble![n: Int] => println![m = n * 2]
			""")
		}
	}
})
