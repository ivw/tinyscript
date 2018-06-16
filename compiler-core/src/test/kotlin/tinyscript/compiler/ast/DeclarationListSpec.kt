package tinyscript.compiler.ast

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import tinyscript.compiler.util.SafeLazy

object DeclarationListSpec : Spek({
	describe("FunctionDefinition") {
		it("can define a pure function with a pure expression") {
			assertAnalysis("""
				getOne = 1
			""")
			assertAnalysis("""
				double[n: Int] = n * 2
			""")
			assertAnalysis("""
				Int.square = this * this
			""")
			assertAnalysis("""
				Int.times[n: Int] = this * n
			""")
			assertAnalysis("""
				[] + [] = left
			""")
		}
		it("can define an impure function with mutable input") {
			assertAnalysis("""
				System!.printEmptyLine! = println!
			""")
			assertAnalysis("""
				System!.printDouble![n: Int] = println![m = n * 2]
			""")
			assertAnalysis("""
				System! +! [] = left.println![m = left]
			""")
		}
		it("can not define a pure function with mutable input") {
			assertAnalysisFails(FunctionImpureException::class, """
				System!.foo = 1
			""")
			assertAnalysisFails(FunctionImpureException::class, """
				foo[s: System!] = 1
			""")
			assertAnalysisFails(FunctionImpureException::class, """
				System! + [] = 1
			""")
		}
		it("can define an impure function with mutable output") {
			assertAnalysis("""
				createIntBox! = intBox![initialValue = 0]
			""")
			assertAnalysis("""
				createIntBox![initialValue: Int] = intBox!
			""")
			assertAnalysis("""
				[] +! [] = intBox!
			""")
		}
		it("can not define a pure function with mutable output") {
			assertAnalysisFails(FunctionImpureException::class, """
				createIntBox = intBox![initialValue = 0]
			""")
			assertAnalysisFails(FunctionImpureException::class, """
				createIntBox[initialValue: Int] = intBox!
			""")
			assertAnalysisFails(FunctionImpureException::class, """
				[] + [] = intBox!
			""")
		}
		it("can not be recursive") {
			assertAnalysisFails(SafeLazy.CycleException::class, """
				getOne = getOne
			""")
			assertAnalysisFails(SafeLazy.CycleException::class, """
				double[n: Int] = double[n = n * 2]
			""")
			assertAnalysisFails(SafeLazy.CycleException::class, """
				[] + [] = left + right
			""")
		}
	}
})
