package tinyscript.compiler.ast

import tinyscript.compiler.ast.parser.TinyScriptParser
import tinyscript.compiler.scope.*

sealed class Expression {
	abstract val type: Type

	abstract val isImpure: Boolean
}

class BlockExpression(
	val statementList: StatementList?,
	val expression: Expression
) : Expression() {
	override val type get() = expression.type

	override val isImpure: Boolean =
		(statementList?.hasImpureImperativeStatement ?: false) || expression.isImpure
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

fun TinyScriptParser.ExpressionContext.analyse(scope: Scope): Expression = when (this) {
	is TinyScriptParser.BlockExpressionContext ->
		block().analyse(scope)
	is TinyScriptParser.IntegerLiteralExpressionContext ->
		IntExpression(text.toInt())
	is TinyScriptParser.FloatLiteralExpressionContext ->
		FloatExpression(text.toDouble())
	is TinyScriptParser.StringLiteralExpressionContext -> TODO()
	is TinyScriptParser.NameReferenceExpressionContext -> {
		val name: String = Name().text
		val isImpure: Boolean = Impure() != null
		val argumentsObjectExpression: ObjectExpression? = `object`()?.analyse(scope)
		val valueEntity: ValueEntity = scope.findValueEntity(NameSignature(
			null,
			name,
			isImpure,
			argumentsObjectExpression?.type
		))
			?: throw AnalysisException("unresolved reference")
		NameReferenceExpression(
			name,
			isImpure,
			argumentsObjectExpression,
			valueEntity.getType()
		)
	}
	is TinyScriptParser.ObjectExpressionContext ->
		`object`().analyse(scope)
	else -> TODO()
}

fun TinyScriptParser.BlockContext.analyse(scope: Scope): BlockExpression {
	val statementCollection = statementList()?.analyse(scope)
	return BlockExpression(
		statementCollection,
		expression().analyse(statementCollection?.scope ?: scope)
	)
}

fun TinyScriptParser.ObjectContext.analyse(scope: Scope): ObjectExpression =
	ObjectExpression(objectStatement().map { it.analyse(scope) })
