package tinyscript.compiler.ast

import tinyscript.compiler.parser.TinyScriptParser
import tinyscript.compiler.scope.Scope

sealed class ObjectStatement

class ObjectFieldDefinition(
	val name: String,
	val expression: Expression
) : ObjectStatement()

class ObjectInheritStatement(
	val expression: Expression
) : ObjectStatement()

fun TinyScriptParser.ObjectStatementContext.analyse(scope: Scope): ObjectStatement =
	when (this) {
		is TinyScriptParser.ObjectFieldDefinitionContext ->
			ObjectFieldDefinition(
				Name().text,
				expression().analyse(scope)
			)
		is TinyScriptParser.ObjectInheritStatementContext -> TODO()
		else -> throw RuntimeException("unknown object statement class")
	}
