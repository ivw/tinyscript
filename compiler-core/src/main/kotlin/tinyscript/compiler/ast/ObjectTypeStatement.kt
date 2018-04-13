package tinyscript.compiler.ast

import tinyscript.compiler.ast.parser.TinyScriptParser
import tinyscript.compiler.scope.Scope

sealed class ObjectTypeStatement

class ObjectTypeFieldDeclaration(
	val name: String,
	val typeExpression: TypeExpression
) : ObjectTypeStatement()

class ObjectTypeInheritStatement(
	val name: String
) : ObjectTypeStatement()

fun TinyScriptParser.ObjectTypeStatementContext.analyse(scope: Scope): ObjectTypeStatement =
	when (this) {
		is TinyScriptParser.ObjectTypeFieldDeclarationContext ->
			ObjectTypeFieldDeclaration(
				Name().text,
				typeExpression().analyse(scope)
			)
		is TinyScriptParser.ObjectTypeInheritStatementContext -> TODO()
		else -> throw RuntimeException("unknown object type statement class")
	}
