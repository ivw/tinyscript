package tinyscript.compiler.stdlib

import org.antlr.v4.runtime.CharStreams
import tinyscript.compiler.ast.analysePure
import tinyscript.compiler.parser.parseFile
import tinyscript.compiler.scope.Scope
import tinyscript.compiler.scope.Type

object StandardLibrary {
	val scope: Scope = run {

		val resource = StandardLibrary::class.java.getResource(
			"/tinyscript/compiler/stdlib/StandardLibrary.tiny"
		)

		println("Reading and parsing the standard library")
		val fileCtx = resource.openStream().use {
			parseFile(CharStreams.fromStream(it))
		}
		println("Parsing done\n")

		val statementList = fileCtx.statementList().statement().analysePure(null)

		statementList.scope
	}

	val intType: Type = scope.findType("Int")!!.type

	val floatType: Type = scope.findType("Float")!!.type
}
