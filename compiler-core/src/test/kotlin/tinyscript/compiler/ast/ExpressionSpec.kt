package tinyscript.compiler.ast

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import tinyscript.compiler.scope.OutsideMutableStateException

object ExpressionSpec : Spek({
	describe("ObjectExpression") {
		it("works") {
			assertAnalysis("""
				foo = []
			""")
			assertAnalysis("""
				foo = [ a = 1, b = 2 ]
			""")
			assertAnalysis("""
				foo = [
					a = 1
					b = 2
				]
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				foo = [ a = 1, b = a ]
			""")
		}
		it("is mutable if one of the fields is mutable") {
			assertAnalysis("""
				foo! = [ a = new intBox, b = 2 ]
			""")
			assertAnalysisFails(FunctionMutableOutputException::class, """
				foo = [ a = new intBox, b = 2 ]
			""")
		}
	}

	describe("(Dot)NameCallExpression") {
		it("can refer to a function") {
			assertAnalysis("""
				a = 1
				main = a
			""")
			assertAnalysis("""
				main = a
				a = 1
			""")
			assertAnalysis("""
				a = 1
				main = (
					b = 1
					a
					a
				)
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				a = 1
				main = b
			""")
			assertAnalysis("""
				Int.a[i: Int] = 1
				main = 3.a[i = 1]
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				Int.a[i: Int] = 1
				main = 3.b[i = 1]
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				Int.a[i: Int] = 1
				main = 3.a[]
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				Int.a[i: Int] = 1
				main = ().a[i = 1]
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				Int.a[i: Int] = 1
				main = 3.a![i = 1]
			""")
		}
		it("can refer to a block field") {
			assertAnalysis("""
				main = (
					a = 1
					a
				)
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				main = (
					a
					a = 1
					a
				)
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				main = (
					a = 1
					b
				)
			""")
		}
		it("can refer to an object field") {
			assertAnalysis("""
				obj = [ a = 1 ]
				main = obj.a
			""")
			assertAnalysis("""
				main = [ a = 1 ].a
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				obj = [ a = 1 ]
				main = obj.b
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				obj = [ a = 1 ]
				main = obj.a!
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				main = [ a = 1 ].b
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				main = ().a
			""")
		}
		it("can refer to a parameter") {
			assertAnalysis("""
				foo[a: Int] = a * 2
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				foo[a: Int] = b * 2
			""")
		}
		it("can refer to `this`") {
			assertAnalysis("""
				Int.a = this * 2
			""")
		}
		it("can refer to a `this` function") {
			assertAnalysis("""
				System!.main! = this.println![m = "Hello"]
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				System!.main! = this.println[m = "Hello"]
			""")
		}
		it("can refer to a `this` object field") {
			assertAnalysis("""
				[a: Int].foo = this.a * 2
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				[a: Int].foo = this.b * 2
			""")
		}
	}

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
