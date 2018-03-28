package tinyscript.compiler.core

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

object RecursionSpec : Spek({
	describe("real declaration") {
		it("can handle recursion") {
			assertAnalysis("""
				getFactorial[n: Int] = if
					(n == 1) 1
					else (n * getFactorial[n - 1])
			""")
			assertAnalysisFails("""
				getFactorial[n: Int] = if
					(n == 1) 1
					else (n || getFactorial[n - 1])
			""")
			assertAnalysisFails("""
				invalidDecl = invalidDecl
			""")
			assertAnalysisFails("""
				invalidDecl: Int = invalidDecl
			""")
			assertAnalysis("""
				root = foo[]

				foo[] = if
					(rand!) bar[]
					else 1

				bar[] = if
					(rand!) foo[]
					else "b"
			""")
			assertAnalysisFails("""
				root = foo[]

				foo[] = bar[]

				bar[] = foo[]
			""")
			assertAnalysis("""
				root = foo[]

				foo[] = if
					(rand!) bar[]
					else 1

				bar[] = foo[]
			""")
			assertAnalysis("""
				root = foo[]

				foo[] = bar[]

				bar[] = if
					(rand!) foo[]
					else "b"
			""")
		}
	}
	describe("type declaration") {
		it("can handle recursion") {
			assertAnalysisFails("""
				type Node = (Node)
			""")
			assertAnalysis("""
				type Node = Node?
				foo: Foo = <Foo>?
				foo2: Foo = foo
			""")
			assertAnalysisFails("""
				type Node = ([ parent: Node ])
			""")
			assertAnalysis("""
				type Node = ([ parent: Node? ])
			""")
//			assertAnalysisFails("""
//				type Node = (Array<Node>)
//			""")
			assertAnalysis("""
				type Node = ([
					foo: [otherNode: Node] -> ?
				])
			""")
			assertAnalysis("""
				type ParentContainer = [
					parent: Node?
				]

				type Node = [
					parentContainer: ParentContainer
					n: Int
				]
			""")
		}
	}
})
