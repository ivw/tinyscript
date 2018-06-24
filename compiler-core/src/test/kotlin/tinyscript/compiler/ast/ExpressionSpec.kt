package tinyscript.compiler.ast

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import tinyscript.compiler.scope.OutsideMutableStateException

object ExpressionSpec : Spek({
	describe("AnonymousFunctionCallExpression") {
		it("can call a pure anonymous function without parameters") {
			assertAnalysis("""
				returnOne = -> 1
				main = returnOne. * 2
			""")
			assertAnalysisFails(OperatorSignatureNotFoundException::class, """
				returnOne = -> 1
				main = returnOne * 2
			""")
			assertAnalysisFails(InvalidAnonymousFunctionCallException::class, """
				returnOne = -> 1
				main = 3.
			""")
			assertAnalysisFails(InvalidAnonymousFunctionCallException::class, """
				returnOne = -> 1
				main = returnOne.!
			""")
			assertAnalysisFails(InvalidAnonymousFunctionCallException::class, """
				returnOne = -> 1
				main = returnOne.[]
			""")
		}
		it("can call an impure anonymous function without parameters") {
			assertAnalysis("""
				main! = (! -> new intBox).!
			""")
			assertAnalysisFails(InvalidAnonymousFunctionCallException::class, """
				main! = (! -> new intBox).
			""")
			assertAnalysisFails(InvalidAnonymousFunctionCallException::class, """
				main! = (! -> new intBox).![]
			""")
		}
		it("can call a pure anonymous function with parameters") {
			assertAnalysis("""
				multiplyByTwo = [n: Int] -> n * 2
				main = (multiplyByTwo.[n = 3]) * 2
			""")
			assertAnalysisFails(InvalidAnonymousFunctionCallException::class, """
				multiplyByTwo = [n: Int] -> n * 2
				main = multiplyByTwo.![n = 3]
			""")
			assertAnalysisFails(InvalidAnonymousFunctionCallException::class, """
				multiplyByTwo = [n: Int] -> n * 2
				main = multiplyByTwo.[]
			""")
			assertAnalysisFails(InvalidAnonymousFunctionCallException::class, """
				multiplyByTwo = [n: Int] -> n * 2
				main = multiplyByTwo.
			""")
		}
	}

	describe("AnonymousFunctionExpression") {
		it("must have ! if it has mutable input or output") {
			assertAnalysis("""
				foo = -> 1
			""")
			assertAnalysis("""
				foo = -> (
					i = new intBox
					i.set![value = 1]
					i.get!
				)
			""")
			assertAnalysis("""
				foo = [] -> 1
			""")
			assertAnalysis("""
				foo! = ! -> 1
			""")
			assertAnalysis("""
				foo! = ! -> new intBox
			""")
			assertAnalysisFails(AnonymousFunctionMutableOutputException::class, """
				foo = -> new intBox
			""")
			assertAnalysis("""
				foo! = ![system: System!] -> system.println!
			""")
		}
		it("is mutable if it is impure") {
			assertAnalysis("""
				returnOne! = ! -> 1
			""")
			assertAnalysisFails(FunctionMutableOutputException::class, """
				returnOne = ! -> 1
			""")
		}
		it("can only use outside mutable values if it doesn't have `!`") {
			assertAnalysis("""
				System!.main! = (
					doPrintln = ! -> this.println!
					doPrintln.!
					doPrintln.!
				)
			""")
			assertAnalysisFails(OutsideMutableStateException::class, """
				System!.main! = (
					doPrintln = -> this.println!
					doPrintln.
					doPrintln.
				)
			""")
		}
	}
})
