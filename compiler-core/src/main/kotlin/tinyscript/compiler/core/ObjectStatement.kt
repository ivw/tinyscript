package tinyscript.compiler.core

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
