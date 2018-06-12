package tinyscript.compiler.ast

import tinyscript.compiler.parser.TinyScriptParser
import tinyscript.compiler.scope.Scope

class BlockStatement(
	val name: String?,
	val expression: Expression
)

fun TinyScriptParser.BlockStatementContext.analyse(scope: Scope): BlockStatement =
	BlockStatement(Name()?.text, expression().analyse(scope))
