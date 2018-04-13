package tinyscript.compiler.ast

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

object ClassExpressionSpec : Spek({
	describe("class expression") {
		it("can be inherited from") {
			assertAnalysis("""
				Animal = class [ name: String ]

				Dog = class [
					&Animal
					bark = -> println[m = "woof"]
				]

				myAnimal: Animal = [&Dog, name = "Foo"]
			""")
			assertAnalysisFails("""
				Animal = class [ name: String ]

				Dog = class [
					&Animal
					bark = -> println[m = "woof"]
				]

				myDog = [&Dog]
			""")
		}

		it("can be used with an explicit type") {
			assertAnalysis("""
				Animal = class [ name: String ]
				myObjectWithName: [name: String] = [&Animal, name = "Foo"]
			""")
			assertAnalysisFails("""
				Animal = class [ name: String ]
				myAnimal: Animal = [name = "Foo"]
			""")
			assertAnalysisFails("""
				Animal = class [ name: String ]
				myAnimal: [&Animal] = [name = "Foo"]
			""")
		}

		it("can have multiple inheritance") {
			assertAnalysis("""
				Scanner = class [ scan: -> ? ]
				Printer = class [ print: -> ? ]
				Copier = class [
					&Scanner
					&Printer

					copy = -> (
						scan[]
						print[]
					)
				]
				foo: [&Scanner, &Printer] = [
					&Copier
					scan = -> println[]
					print = -> println[]
				]
			""")
		}

		it("can use forward references until it is used") {
			assertAnalysis("""
				Animal = class [
					name: String = defaultAnimalName
				]

				defaultAnimalName = "Foo"

				myAnimal = [&Animal]
			""")
		}
	}
})
