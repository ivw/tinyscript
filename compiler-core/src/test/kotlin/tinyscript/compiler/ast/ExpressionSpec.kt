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
				assertAnalysisFails("""
					foo = 2 * ()
				""", OperatorSignatureNotFoundException::class)
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
				assertAnalysisFails("""
					foo = (()) * 2
				""", OperatorSignatureNotFoundException::class)
			}
			it("can be nested") {
				assertAnalysis("""
					foo = (((((3))) * (2)))
				""")
			}
			it("is impure if the inner expression is impure") {
				assertAnalysisFails("""
					foo = (println![])
				""", DisallowedImpureStatementException::class)
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
				assertAnalysisFails("""
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
				""", DisallowedImpureStatementException::class)
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
		it("can refer to fields") {
			assertAnalysis("""
				a = 1
				a
			""")
			assertAnalysis("""
				a
				a = 1
			""")
			assertAnalysisFails("""
				a = 1
				b
			""", NameSignatureNotFoundException::class)
		}
		it("can refer to pure functions without parameters") {
			assertAnalysis("""
				a => 1
				a
			""")
			assertAnalysis("""
				a
				a => 1
			""")
			assertAnalysisFails("""
				a => 1
				b
			""", NameSignatureNotFoundException::class)
			assertAnalysisFails("""
				a! => 1
				a
			""", NameSignatureNotFoundException::class)
		}
		it("can refer to impure functions without parameters") {
			assertAnalysis("""
				a! => 1
				a!
			""", true)
			assertAnalysis("""
				a!
				a! => 1
			""", true)
			assertAnalysisFails("""
				a! => 1
				b!
			""", NameSignatureNotFoundException::class, true)
			assertAnalysisFails("""
				a => 1
				a!
			""", NameSignatureNotFoundException::class, true)
		}
	}
})
