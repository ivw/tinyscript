package tinyscript.compiler.ast

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import tinyscript.compiler.util.SafeLazy

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
			assertAnalysisFails(DisallowedImpureStatementException::class, """
				println!
			""")
			assertAnalysisFails(DisallowedImpureStatementException::class, """
				result = println!
			""")
		}
		it("can state an impure expression in impure scope") {
			assertAnalysis("""
				main! => (
					println!
					0
				)
			""")
			assertAnalysis("""
				main! => (
					result = println!
					0
				)
			""")
		}
		it("can not be recursive") {
			assertAnalysisFails(SafeLazy.CycleException::class, """
				a = a
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
			assertAnalysis("""
				[] + [] => left
			""")
		}
		it("can not declare a pure function with an impure expression") {
			assertAnalysisFails(PureFunctionWithImpureExpressionException::class, """
				printEmptyLine => println!
			""")
			assertAnalysisFails(PureFunctionWithImpureExpressionException::class, """
				printDouble[n: Int] => println![m = n * 2]
			""")
			assertAnalysisFails(PureFunctionWithImpureExpressionException::class, """
				[] + [] => println![m = left]
			""")
		}
		it("can declare an impure function with an impure expression") {
			assertAnalysis("""
				printEmptyLine! => println!
			""")
			assertAnalysis("""
				printDouble![n: Int] => println![m = n * 2]
			""")
			assertAnalysis("""
				[] +! [] => println![m = left]
			""")
		}
		it("can not be recursive") {
			assertAnalysisFails(SafeLazy.CycleException::class, """
				getOne => getOne
			""")
			assertAnalysisFails(SafeLazy.CycleException::class, """
				double[n: Int] => double[n = n * 2]
			""")
			assertAnalysisFails(SafeLazy.CycleException::class, """
				[] + [] => left + right
			""")
		}
	}
})
