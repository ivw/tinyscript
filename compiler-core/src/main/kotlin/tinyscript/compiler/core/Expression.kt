package tinyscript.compiler.core

sealed class Expression {
	abstract val type: Type

	abstract val isImpure: Boolean
}

class BlockExpression(
	val declarationCollection: DeclarationCollection?,
	val expression: Expression
) : Expression() {
	override val type get() = expression.type

	override val isImpure: Boolean =
		(declarationCollection?.hasImpureDeclarations ?: false) || expression.isImpure
}

class IntExpression(val value: Int) : Expression() {
	override val type = IntType(value, value)
	override val isImpure: Boolean get() = false
}

class FloatExpression(val value: Double) : Expression() {
	override val type = FloatType(value, value)
	override val isImpure: Boolean get() = false
}

class NullExpression(val nonNullType: Type) : Expression() {
	override val type: Type = NullableType(nonNullType)
	override val isImpure: Boolean get() = false
}

class ObjectExpression(val declarationCollection: DeclarationCollection?) : Expression() {
	override val type = ObjectType(
		declarationCollection?.let { it.scope.entityCollection },
		emptySet() // TODO
	)

	override val isImpure: Boolean
		get() = declarationCollection?.hasImpureDeclarations ?: false
}

class ReferenceExpression(
	val name: String,
	override val isImpure: Boolean,
	override val type: Type
) : Expression()

class FunctionCallExpression(
	val name: String,
	val argumentsObjectExpression: ObjectExpression,
	override val isImpure: Boolean,
	override val type: Type
) : Expression()
