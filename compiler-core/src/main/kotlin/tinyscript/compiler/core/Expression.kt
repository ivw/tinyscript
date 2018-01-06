package tinyscript.compiler.core

sealed class Expression {
	abstract val type: Type

	open val isImpure: Boolean = false
}

class BlockExpression(val blockScope: Scope, val expression: Expression) : Expression() {
	override val type get() = expression.type
}

class IntExpression(val value: Int) : Expression() {
	override val type = IntType(value, value)
}
