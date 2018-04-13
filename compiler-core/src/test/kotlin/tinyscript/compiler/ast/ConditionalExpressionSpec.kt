package tinyscript.compiler.ast

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

object ConditionalExpressionSpec : Spek({
	describe("conditional expression") {
		it("has the type of all expression types intersected") {
			assertAnalysis("""
				Animal = class [ name: String ]

				Dog = class [
					&Animal
					bark = -> println[m = "woof"]
				]

				Cat = class [
					&Animal
					meow = -> println[m = "meow"]
				]

				myAnimal = if
					(true) [&Dog, name = "Foo"]
					(false) [&Animal, name = "Foo"]
					else [&Cat, name = "Bar"]
				println[m = myAnimal.name]
			""")
			assertAnalysisFails("""
				foo: String = if (true) "abc" else 3
			""")
			assertAnalysisFails("""
				myObject = if (true) [ a = 1, foo = 3 ] else [ b = 2, foo = 4 ]
				myObject.foo
			""")
		}
	}
})
