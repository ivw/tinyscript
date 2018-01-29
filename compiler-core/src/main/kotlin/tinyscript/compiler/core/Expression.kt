package tinyscript.compiler.core

sealed class Expression {
	abstract val type: Type

	abstract val isImpure: Boolean
}

class BlockExpression(val declarationCollection: DeclarationCollection, val expression: Expression) : Expression() {
	override val type get() = expression.type

	override val isImpure: Boolean
		get() = TODO("not implemented")
}

class IntExpression(val value: Int) : Expression() {
	override val type = IntType(value, value)

	override val isImpure: Boolean get() = false
}

class ObjectExpression(val declarationCollection: DeclarationCollection) : Expression() {
	override val type = ObjectType(declarationCollection.scope.entities, TODO())

	override val isImpure: Boolean
		get() = TODO("not implemented")
}
