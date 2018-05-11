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
			}
			it("can be nested") {
				assertAnalysis("""
					foo = (((((3))) * (2)))
				""")
			}
			it("is impure if the inner expression is impure") {
				assertAnalysisFails(DisallowedImpureStatementException::class, """
					foo = (println![])
				""")
			}
		}

		describe("block with statements") {
			it("returns the final expression type") {
				assertAnalysis("""
					total = (
						getN[] => 2
						n = getN[]
						n * n
					) * 2
				""")
				assertAnalysis("""
					total = ( n = 2, n * n ) * 2
				""")
			}
			it("is impure if the inner expression is impure or one of the statements is impure") {
				assertAnalysisFails(DisallowedImpureStatementException::class, """
					foo = (
						println![]
						1
					)
					foo = (
						a = 1
						println![]
					)
					foo = (
						println![]
						println![]
					)
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
				foo = [ a = 1 ]
			""")
		}
	}

	describe("NameReferenceExpression") {
		it("can refer to a field") {
			assertAnalysis("""
				a = 1
				a
			""")
			assertAnalysis("""
				a
				a = 1
			""")
			assertAnalysis("""
				a = 1
				(
					b = 1
					a
					a
				)
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				a = 1
				b
			""")
		}
		it("can refer to a pure function without parameters") {
			assertAnalysis("""
				a => 1
				a
			""")
			assertAnalysis("""
				a
				a => 1
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				a => 1
				b
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				a! => 1
				a
			""")
		}
		it("can refer to an impure function without parameters") {
			assertAnalysis("""
				a! => 1
				a!
			""", true)
			assertAnalysis("""
				a!
				a! => 1
			""", true)
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				a! => 1
				b!
			""", true)
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				a => 1
				a!
			""", true)
		}
		it("can refer to a pure function with parameters") {
			assertAnalysis("""
				a[] => 1
				a[]
			""")
			assertAnalysis("""
				a[]
				a[] => 1
			""")
			assertAnalysis("""
				a[x: Int] => 1
				a[x = 1]
			""")
			assertAnalysis("""
				a[x: Int] => 1
				a[x = 1, y = 2]
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				a[x: Int] => 1
				a[]
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				a[x: Int] => 1
				a[x = ()]
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				a[] => 1
				b[]
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				a![] => 1
				a[]
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				a => 1
				a[]
			""")
		}
		it("can refer to an impure function with parameters") {
			assertAnalysis("""
				a![] => 1
				a![]
			""", true)
			assertAnalysis("""
				a![]
				a![] => 1
			""", true)
			assertAnalysis("""
				a![x: Int] => 1
				a![x = 1]
			""", true)
			assertAnalysis("""
				a![x: Int] => 1
				a![x = 1, y = 2]
			""", true)
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				a![x: Int] => 1
				a![]
			""", true)
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				a![x: Int] => 1
				a![x = ()]
			""", true)
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				a![] => 1
				b![]
			""", true)
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				a[] => 1
				a![]
			""", true)
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				a! => 1
				a![]
			""")
		}
	}

	describe("DotNameReferenceExpression") {
		it("can refer to object fields") {
			assertAnalysis("""
				animal = [name = "Foo"]
				animal.name
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				animal = [name = "Foo"]
				animal.age
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				animal = [name = "Foo"]
				animal.name!
			""")
		}
		it("can refer to a pure function without parameters") {
			assertAnalysis("""
				Int.square => this * this
				2.square
			""")
			assertAnalysis("""
				Int.a => 1
				(
					a = ()

					3.a * 2
				)
			""")
			assertAnalysis("""
				[foo: Int].a => foo
				[foo = 123].a
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				[foo: Int].a => bar
				[foo = 123].a
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				Int.square => this * this
				().square
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				Int.square => this * this
				2.square!
			""")
		}
		it("can refer to an impure function without parameters") {
			assertAnalysis("""
				Int.print! => println![]
				2.print!
			""", true)
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				Int.print! => println![]
				().print!
			""", true)
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				Int.print! => println![]
				2.print
			""", true)
		}
		it("can refer to a pure function with parameters") {
			assertAnalysis("""
				Int.times[n: Int] => this * n
				2.times[n = 3]
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				Int.times[n: Int] => this * n
				().times[n = 3]
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				Int.times[n: Int] => this * n
				2.times![n = 3]
			""")
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				Int.times[n: Int] => this * n
				2.times[n = ()]
			""")
		}
		it("can refer to an impure function with parameters") {
			assertAnalysis("""
				Int.printTimes![n: Int] => println![m = this * n]
				2.printTimes![n = 3]
			""", true)
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				Int.printTimes![n: Int] => println![m = this * n]
				().printTimes![n = 3]
			""", true)
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				Int.printTimes![n: Int] => println![m = this * n]
				2.printTimes[n = 3]
			""", true)
			assertAnalysisFails(NameSignatureNotFoundException::class, """
				Int.printTimes![n: Int] => println![m = this * n]
				2.printTimes![n = ()]
			""", true)
		}
	}

	describe("AnonymousFunctionExpression") {
		it("works") {
			assertAnalysis("""
				multiplyByTwo = [n: Int] -> n * 2

				printTwo = ! -> println![m = 2]
			""")
		}
	}
})
