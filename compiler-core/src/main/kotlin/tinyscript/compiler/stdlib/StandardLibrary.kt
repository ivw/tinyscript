package tinyscript.compiler.stdlib

import org.antlr.v4.runtime.CharStreams
import tinyscript.compiler.ast.analyse
import tinyscript.compiler.parser.parseFile
import tinyscript.compiler.scope.Scope
import tinyscript.compiler.scope.intrinsicsScope

object StandardLibrary {
	val scope: Scope = run {
		val fileCtx = StandardLibrary::class.java.getResourceAsStream(
			"/tinyscript/compiler/stdlib/StandardLibrary.tiny"
		).use {
			parseFile(CharStreams.fromStream(it))
		}

		val declarationList = fileCtx.declaration().analyse(intrinsicsScope)

		declarationList.scope
	}
}
