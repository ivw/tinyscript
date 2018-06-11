package tinyscript.compiler.stdlib

import org.antlr.v4.runtime.CharStreams
import tinyscript.compiler.ast.analyse
import tinyscript.compiler.parser.parseFile
import tinyscript.compiler.scope.Scope
import tinyscript.compiler.scope.intrinsicsScope

object StandardLibrary {
	val scope: Scope = run {
		val resource = StandardLibrary::class.java.getResource(
			"/tinyscript/compiler/stdlib/StandardLibrary.tiny"
		)

		val fileCtx = resource.openStream().use {
			parseFile(CharStreams.fromStream(it))
		}

		val statementList = fileCtx.declaration().analyse(intrinsicsScope)

		statementList.scope
	}
}
