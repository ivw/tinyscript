package tinyscript.compiler.core

import tinyscript.compiler.scope.Type

sealed class TypeExpression {
	abstract val type: Type
}

class ParenTypeExpression(val typeExpression: TypeExpression) : TypeExpression() {
	override val type get() = typeExpression.type
}

class FunctionTypeExpression(
	val objectTypeExpression: ObjectTypeExpression,
	val isImpure: Boolean,
	val returnTypeExpression: TypeExpression
) : TypeExpression() {
	override val type: Type =
}

class ObjectTypeExpression() : TypeExpression() {

}
