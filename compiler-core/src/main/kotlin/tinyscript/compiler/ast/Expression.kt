package tinyscript.compiler.ast

import tinyscript.compiler.parser.TinyScriptParser
import tinyscript.compiler.scope.*

sealed class Expression {
	abstract val type: Type

	abstract val isImpure: Boolean
}

class AnyExpression : Expression() {
	override val type = AnyType
	override val isImpure = false
}

class BlockExpression(
	val statementList: StatementList,
	val expression: Expression
) : Expression() {
	override val type get() = expression.type

	override val isImpure: Boolean =
		statementList.hasImpureImperativeStatement || expression.isImpure
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
	val valueResult: ValueResult
) : Expression() {
	override val type: Type = valueResult.type
}

class OperatorCallExpression(
	val lhsExpression: Expression?,
	val operatorSymbol: String,
	override val isImpure: Boolean,
	val rhsExpression: Expression,
	val valueResult: ValueResult
) : Expression() {
	override val type: Type = valueResult.type
}

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

		val valueResult = scope.findValue(NameSignature(
			null,
			name,
			isImpure,
			argumentsObjectExpression?.type
		))
			?: throw AnalysisException("unresolved reference '$name'")

		NameReferenceExpression(
			name,
			isImpure,
			argumentsObjectExpression,
			valueResult
		)
	}
	is TinyScriptParser.InfixOperatorCallExpressionContext -> {
		val lhsExpression = lhs.analyse(scope)
		val operatorSymbol: String = OperatorSymbol().text
		val isImpure: Boolean = Impure() != null
		val rhsExpression = rhs.analyse(scope)

		val valueResult = scope.findValue(OperatorSignature(
			lhsExpression.type,
			operatorSymbol,
			isImpure,
			rhsExpression.type
		))
			?: throw AnalysisException("unresolved reference '$operatorSymbol'")

		OperatorCallExpression(
			lhsExpression,
			operatorSymbol,
			isImpure,
			rhsExpression,
			valueResult
		)
	}
	is TinyScriptParser.ObjectExpressionContext ->
		`object`().analyse(scope)
	else -> TODO()
}

fun TinyScriptParser.BlockContext.analyse(scope: Scope): Expression {
	val expressionCtx = expression()
		?: return AnyExpression()

	val statementListCtx = statementList()
		?: return expressionCtx.analyse(scope)

	val statementCollection = statementListCtx.analyse(scope)
	return BlockExpression(
		statementCollection,
		expressionCtx.analyse(statementCollection.scope)
	)
}

fun TinyScriptParser.ObjectContext.analyse(scope: Scope): ObjectExpression =
	ObjectExpression(objectStatement().map { it.analyse(scope) })
