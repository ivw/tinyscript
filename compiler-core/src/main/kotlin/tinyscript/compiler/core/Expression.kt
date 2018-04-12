package tinyscript.compiler.core

import tinyscript.compiler.scope.FloatType
import tinyscript.compiler.scope.IntType
import tinyscript.compiler.scope.ObjectType
import tinyscript.compiler.scope.Type

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
		(statementCollection?.hasImpureImperativeStatement ?: false) || expression.isImpure
}

class IntExpression(val value: Int) : Expression() {
	override val type = IntType(value, value)
	override val isImpure: Boolean get() = false
}

class FloatExpression(val value: Double) : Expression() {
	override val type = FloatType(value, value)
	override val isImpure: Boolean get() = false
}

class ObjectExpression(val objectStatements: List<ObjectStatement>) : Expression() {
	override val type = ObjectType(mutableMapOf<String, Type>().also { mutableFieldMap ->
		objectStatements.forEach { objectStatement ->
			when (objectStatement) {
				is ObjectFieldDeclaration -> {
					mutableFieldMap[objectStatement.name] = objectStatement.expression.type
				}
				is ObjectInheritStatement -> {
					TODO()
				}
			}
		}
	})

	override val isImpure: Boolean =
		objectStatements.any { it.isImpure }
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
