package tinyscript.compiler.ast

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

object ExpressionSpec : Spek({
	describe("BlockExpression") {
		describe("empty block (any-expression)") {
			it("works") {
				assertAnalysis("""
					foo = ()
				""")
				assertAnalysis("""
					foo = (

					)
				""")
			}

			it("can not be used for anything") {
				assertAnalysisFails(OperatorSignatureNotFoundException::class, """
					foo = 2 * ()
				""")
			}
		}

		describe("block without statements") {
			it("works") {
				assertAnalysis("""
					foo = (3) * 2
				""")
				assertAnalysis("""
					foo = (

						3

					) * 2
				""")
				assertAnalysisFails(OperatorSignatureNotFoundException::class, """
					foo = (()) * 2
				""")
				assertAnalysisFails(FunctionImpureException::class, """
					foo = (intBox!)
				""")
			}
			it("can be nested") {
				assertAnalysis("""
					foo = (((((3))) * (2)))
				""")
			}
		}

		describe("block with statements") {
			it("returns the final expression type") {
				assertAnalysis("""
					total = (
						n = intBox!
						n *! 2
						n.get!
					) * 2
				""")
				assertAnalysis("""
					total = ( n = 2, n * n ) * 2
				""")
			}
		}
	}

	describe("IntegerLiteralExpression") {
		it("works") {
			assertAnalysis("""
				a = 1
			""")
		}
	}

	describe("FloatLiteralExpression") {
		it("works") {
			assertAnalysis("""
				a = 1.0
			""")
		}
	}

	describe("StringLiteralExpression") {
		it("works") {
			assertAnalysis("""
				foo = "bar"
			""")
		}
	}

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
				foo! = [ a = intBox!, b = 2 ]
			""")
			assertAnalysisFails(FunctionImpureException::class, """
				foo = [ a = intBox!, b = 2 ]
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
				System!.main! = println![m = "Hello"]
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				System!.main! = println[m = "Hello"]
			""")
		}
		it("can refer to a `this` object field") {
			assertAnalysis("""
				[a: Int].foo = a * 2
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				[a: Int].foo = b * 2
			""")
		}
	}

//	describe("AnonymousFunctionCallExpression") {
//		it("can call a pure anonymous function without parameters") {
//			assertAnalysis("""
//				returnOne = -> 1
//				returnOne. * 2
//			""")
//			assertAnalysisFails(OperatorSignatureNotFoundException::class, """
//				returnOne = -> 1
//				returnOne * 2
//			""")
//			assertAnalysisFails(InvalidAnonymousFunctionCallException::class, """
//				returnOne = -> 1
//				3.
//			""")
//			assertAnalysisFails(InvalidAnonymousFunctionCallException::class, """
//				returnOne = -> 1
//				returnOne.!
//			""")
//			assertAnalysisFails(InvalidAnonymousFunctionCallException::class, """
//				returnOne = -> 1
//				returnOne.[]
//			""")
//			assertAnalysisFails(DisallowedImpureStatementException::class, """
//				printSomethingAndReturnPureAnonymousFunc! => (
//					println!
//					-> 1
//				)
//				(printSomethingAndReturnPureAnonymousFunc!).
//			""")
//		}
//		it("can call an impure anonymous function without parameters") {
//			assertAnalysis("""
//				printOne = ! -> println![m = 1]
//				printOne.!
//			""", true)
//			assertAnalysisFails(InvalidAnonymousFunctionCallException::class, """
//				printOne = ! -> println![m = 1]
//				printOne.
//			""", true)
//			assertAnalysisFails(InvalidAnonymousFunctionCallException::class, """
//				printOne = ! -> println![m = 1]
//				printOne.![]
//			""", true)
//		}
//		it("can call a pure anonymous function with parameters") {
//			assertAnalysis("""
//				multiplyByTwo = [n: Int] -> n * 2
//				(multiplyByTwo.[n = 3]) * 2
//			""")
//			assertAnalysisFails(InvalidAnonymousFunctionCallException::class, """
//				multiplyByTwo = [n: Int] -> n * 2
//				multiplyByTwo.![n = 3]
//			""")
//			assertAnalysisFails(InvalidAnonymousFunctionCallException::class, """
//				multiplyByTwo = [n: Int] -> n * 2
//				multiplyByTwo.[]
//			""")
//			assertAnalysisFails(InvalidAnonymousFunctionCallException::class, """
//				multiplyByTwo = [n: Int] -> n * 2
//				multiplyByTwo.
//			""")
//			assertAnalysisFails(DisallowedImpureStatementException::class, """
//				printSomethingAndReturnPureAnonymousFunc! => (
//					println!
//					[] -> 1
//				)
//				(printSomethingAndReturnPureAnonymousFunc!).[]
//			""")
//			assertAnalysisFails(DisallowedImpureStatementException::class, """
//				foo = [n: ()] -> 123
//				foo.[n = println!]
//			""")
//		}
//		it("can call an impure anonymous function with parameters") {
//			assertAnalysis("""
//				printDouble = ![n: Int] -> println![m = n * 2]
//				printDouble.![n = 2]
//			""", true)
//			assertAnalysisFails(InvalidAnonymousFunctionCallException::class, """
//				printDouble = ![n: Int] -> println![m = n * 2]
//				printDouble.[n = 2]
//			""", true)
//			assertAnalysisFails(InvalidAnonymousFunctionCallException::class, """
//				printDouble = ![n: Int] -> println![m = n * 2]
//				printDouble.![]
//			""", true)
//			assertAnalysisFails(InvalidAnonymousFunctionCallException::class, """
//				printDouble = ![n: Int] -> println![m = n * 2]
//				printDouble.!
//			""", true)
//		}
//	}
//
//	describe("AnonymousFunctionExpression") {
//		it("can express a pure anonymous function without parameters") {
//			assertAnalysis("""
//				returnOne = -> 1
//			""")
//			assertAnalysisFails(PureFunctionWithImpureExpressionException::class, """
//				returnOne = -> println![m = 1]
//			""")
//		}
//		it("can express an impure anonymous function without parameters") {
//			assertAnalysis("""
//				printTwo = ! -> println![m = 2]
//			""")
//		}
//		it("can express a pure anonymous function with parameters") {
//			assertAnalysis("""
//				multiplyByTwo = [n: Int] -> n * 2
//			""")
//			assertAnalysisFails(PureFunctionWithImpureExpressionException::class, """
//				multiplyByTwo = [n: Int] -> println![m = n]
//			""")
//		}
//		it("can express an impure anonymous function with parameters") {
//			assertAnalysis("""
//				printDouble = ![n: Int] -> println![m = n * 2]
//			""")
//		}
//	}
})
