package tinyscript.compiler.ast

import tinyscript.compiler.parser.TinyScriptParser
import tinyscript.compiler.scope.Scope

sealed class ObjectStatement {
	abstract val isImpure: Boolean
}

class ObjectFieldDeclaration(
	val name: String,
	val expression: Expression
) : ObjectStatement() {
	override val isImpure: Boolean get() = expression.isImpure
}

class ObjectInheritStatement(
	val expression: Expression
) : ObjectStatement() {
	override val isImpure: Boolean get() = expression.isImpure
}

fun TinyScriptParser.ObjectStatementContext.analyse(scope: Scope): ObjectStatement =
	when (this) {
		is TinyScriptParser.ObjectFieldDeclarationContext ->
			ObjectFieldDeclaration(
				Name().text,
				expression().analyse(scope)
			)
		is TinyScriptParser.ObjectInheritStatementContext -> TODO()
		else -> throw RuntimeException("unknown object statement class")
	}
