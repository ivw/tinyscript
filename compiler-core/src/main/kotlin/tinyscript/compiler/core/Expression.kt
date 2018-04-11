package tinyscript.compiler.core

import tinyscript.compiler.scope.*

sealed class Expression {
	abstract val type: Type

	abstract val isImpure: Boolean
}

class BlockExpression(
	val statementCollection: StatementCollection?,
	val expression: Expression
) : Expression() {
	override val type get() = expression.type

	override val isImpure: Boolean =
		(statementCollection?.hasImpureDeclarations ?: false) || expression.isImpure
}

class IntExpression(val value: Int) : Expression() {
	override val type = IntType(value, value)
	override val isImpure: Boolean get() = false
}

class FloatExpression(val value: Double) : Expression() {
	override val type = FloatType(value, value)
	override val isImpure: Boolean get() = false
}

class ObjectExpression(val declarationCollection: DeclarationCollection?) : Expression() {
	override val type = ObjectType(
		if (declarationCollection != null)
			declarationCollection.scope.entityCollection
		else EmptyEntityCollection,
		emptySet() // TODO
	)

	override val isImpure: Boolean =
		declarationCollection?.hasImpureDeclarations ?: false
}

class NameReferenceExpression(
	val name: String,
	override val isImpure: Boolean,
	val argumentsObjectExpression: ObjectExpression?,
	override val type: Type
) : Expression()

class FunctionCallExpression(
	val name: String,
	val argumentsObjectExpression: ObjectExpression,
	override val isImpure: Boolean,
	override val type: Type
) : Expression()
