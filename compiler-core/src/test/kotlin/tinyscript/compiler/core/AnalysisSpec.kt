package tinyscript.compiler.core

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import tinyscript.compiler.core.parser.TinyScriptLexer
import tinyscript.compiler.core.parser.TinyScriptParser
import kotlin.test.assertFails

fun analyse(codeString: String) {
	val lexer = TinyScriptLexer(CharStreams.fromString(codeString.trimIndent()))
	val parser = TinyScriptParser(CommonTokenStream(lexer))
	val fileCtx = parser.file()

	if (parser.numberOfSyntaxErrors > 0)
		throw RuntimeException("parsing failed")

	val analysisVisitor = AnalysisVisitor("testCode")
	analysisVisitor.visitFile(fileCtx)
	analysisVisitor.finishDeferredAnalyses()
}

object AnalysisSpec : Spek({
	describe("analysis") {
		it("allows definition with implicit type") {
			analyse("""
				myString = "foo"
			""")
		}

		it("allows definition with explicit type") {
			analyse("""
				myString: String = "foo"
			""")
		}

		it("disallows definition with wrong explicit type") {
			assertFails {
				analyse("""
					myString: Int = "foo"
				""")
			}
		}

		it("allows mutable declaration reassign") {
			analyse("""
				# myString = "foo"
				myString <- "bar"
			""")
		}

		it("disallows immutable declaration reassign") {
			assertFails {
				analyse("""
					myString = "foo"
					myString <- "bar"
				""")
			}
		}

		it("allows valid reassignment of definition with implicit nullable type") {
			analyse("""
				# myField = <String>?
				myField <- "foo"
			""")
		}

		it("disallows invalid reassignment of definition with implicit nullable type") {
			assertFails {
				analyse("""
					# myField = <String>?
					myField <- 123
				""")
			}
		}

		it("allows valid null-assignment of definition with explicit nullable type") {
			analyse("""
				# myField: String? = "foo"
				myField <- <String>?
			""")
		}

		it("disallows invalid null-assignment of definition with non-nullable type") {
			assertFails {
				analyse("""
					# myField = "foo"
					myField <- <String>?
				""")
			}
		}

		it("disallows assigning a nullable value to a non-nullable field") {
			assertFails {
				analyse("""
					myString: String = <String>?
				""")
			}
		}

		it("disallows assigning an invalid null value to a nullable field") {
			assertFails {
				analyse("""
					myField: String? = ?
				""")
			}
		}

		it("disallows using forward references in local scope") {
			assertFails {
				analyse("""
					myField = forwardField

					forwardField = "foo"
				""")
			}
		}

		it("allows creating an object and referencing a field") {
			analyse("""
				myObject = [ foo = "bar" ]
				myObject.foo

				myObjectExplicit: [ foo: String ] = [ foo = "bar" ]
				myObjectExplicit.foo
			""")
		}

		it("disallows invalid object assignment") {
			assertFails {
				analyse("""
					myObject: [ foo: String ]  = [ abc = 123 ]
					myObject.foo
				""")
			}
		}

		it("disallows referencing fields of nullable object") {
			assertFails {
				analyse("""
					myObject: [ foo: String ]?  = [ foo = "bar" ]
					myObject.foo
				""")
			}
		}

		it("disallows referencing a non-existent field of an object") {
			assertFails {
				analyse("""
					myObject: [ foo: String ]  = [ foo = "bar", abc = 123 ]
					myObject.abc
				""")
			}
		}

		it("allows mutable object field reassign") {
			analyse("""
				myObject = [ # foo: String? = <String>? ]
				myObject.foo <- "bar"
			""")
		}

		it("disallows immutable object field reassign") {
			assertFails {
				analyse("""
					myObject = [ foo: String? = <String>? ]
					myObject.foo <- "bar"
				""")
			}
		}

		it("allows creating a class and inheriting from it in objects and classes") {
			analyse("""
				Animal = class [ name: String ]

				Dog = class [
					&Animal
					bark = -> println[m = "woof"]
				]

				myAnimal: Animal = [&Dog, name = "Foo"]
			""")
		}

		it("disallows creating objects without making every field concrete") {
			assertFails {
				analyse("""
					Animal = class [ name: String ]

					Dog = class [
						&Animal
						bark = -> println[m = "woof"]
					]

					myDog = [&Dog]
				""")
			}
		}

		it("allows assigning a class inherited object to a structural object type") {
			analyse("""
				Animal = class [ name: String ]
				myObjectWithName: [name: String] = [&Animal, name = "Foo"]
			""")
		}

		it("disallows disallows assigning an object with missing identities") {
			assertFails {
				analyse("""
					Animal = class [ name: String ]
					myAnimal: Animal = [name = "Foo"]
				""")
			}
		}

		it("allows multiple inheritance") {
			analyse("""
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

		it("allows using forward references in classes") {
			analyse("""
				Animal = class [
					name: String = defaultAnimalName
				]

				defaultAnimalName = "Foo"

				myAnimal = [&Animal]
			""")
		}

		it("disallows inheriting an object in local scope") {
			assertFails {
				analyse("""
					Point = class [ x: Int = 0, y: Int = 0 ]
					&Point
				""")
			}
		}

		it("allows using a block expression") {
			analyse("""
				total: Int = (
					# n = 0
					n = n + 2
					n = n + 3
					n
				)
			""")
		}

		it("allows using functions with and without parameters") {
			analyse("""
				multiplyByTwo = [n: Int] -> n * 2
				double = multiplyByTwo[n = 1]

				sayHi = -> println[m = "Hi"]
				sayHi[]
			""")
		}

		it("disallows using a function with invalid arguments") {
			assertFails {
				analyse("""
					multiplyByTwo = [n: Int] -> n * 2
					foo = multiplyByTwo[]
				""")
			}
		}

		it("allows using forward references in functions") {
			analyse("""
				sayHi = -> println[m = hiMessage]
				hiMessage = "Hi"
				sayHi[]
			""")
		}
	}
})
