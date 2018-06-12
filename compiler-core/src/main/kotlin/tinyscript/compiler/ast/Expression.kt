package tinyscript.compiler.ast

import tinyscript.compiler.parser.TinyScriptParser
import tinyscript.compiler.scope.*

sealed class Expression {
	abstract val type: Type
}

class AnyExpression : Expression() {
	override val type = AnyType
}

class BlockExpression(
	val statementList: StatementList,
	val expression: Expression
) : Expression() {
	override val type get() = expression.type
}

class IntExpression(val value: Int) : Expression() {
	override val type = IntType(value, value)
}

class FloatExpression(val value: Double) : Expression() {
	override val type = FloatType(value, value)
}

class StringExpression(val value: String) : Expression() {
	override val type = stringType
}

class ObjectExpression(val objectStatements: List<ObjectStatement>) : Expression() {
	override val type = ObjectType(objectStatements.fold(mutableMapOf(), { mutableFieldMap, objectStatement ->
		when (objectStatement) {
			is ObjectFieldDefinition -> {
				mutableFieldMap[objectStatement.name] = objectStatement.expression.type
			}
			is ObjectInheritStatement -> {
				TODO()
			}
		}
		mutableFieldMap
	}))
}

class NameReferenceExpression(
	val expression: Expression?,
	val name: String,
	val signatureIsImpure: Boolean,
	val argumentsObjectExpression: ObjectExpression?,
	val valueResult: ValueResult
) : Expression() {
	override val type: Type = valueResult.type
}

class ObjectFieldRefExpression(
	val expression: Expression,
	val name: String,
	override val type: Type
) : Expression()

class AnonymousFunctionCallExpression(
	val expression: Expression,
	val signatureIsImpure: Boolean,
	val argumentsObjectExpression: ObjectExpression?,
	override val type: Type
) : Expression()

class OperatorCallExpression(
	val lhsExpression: Expression?,
	val operatorSymbol: String,
	val operatorIsImpure: Boolean,
	val rhsExpression: Expression,
	val valueResult: ValueResult
) : Expression() {
	override val type: Type = valueResult.type
}

class AnonymousFunctionExpression(
	val isFunctionImpure: Boolean,
	val paramsObjectTypeExpression: ObjectTypeExpression?,
	val returnExpression: Expression
) : Expression() {
	override val type: Type = FunctionType(isFunctionImpure, paramsObjectTypeExpression?.type, returnExpression.type)
}

class ObjectFieldNotFoundException(val name: String) : RuntimeException(
	"unresolved reference '$name'"
)

class NameSignatureNotFoundException(val name: String) : RuntimeException(
	"unresolved reference '$name'"
)

class OperatorSignatureNotFoundException(val operatorSymbol: String) : RuntimeException(
	"unresolved reference '$operatorSymbol'"
)

class InvalidAnonymousFunctionCallException : RuntimeException(
	"invalid anonymous function call"
)

fun TinyScriptParser.ExpressionContext.analyse(scope: Scope): Expression = when (this) {
	is TinyScriptParser.BlockExpressionContext ->
		block().analyse(scope)
	is TinyScriptParser.IntegerLiteralExpressionContext ->
		IntExpression(text.toInt())
	is TinyScriptParser.FloatLiteralExpressionContext ->
		FloatExpression(text.toDouble())
	is TinyScriptParser.StringLiteralExpressionContext ->
		StringExpression(text)
	is TinyScriptParser.NameCallExpressionContext ->
		analyseNameReferenceExpression(
			scope,
			null,
			Name().text,
			Impure() != null,
			`object`()?.analyse(scope)
		)
	is TinyScriptParser.DotNameCallExpressionContext ->
		analyseNameReferenceExpression(
			scope,
			expression().analyse(scope),
			Name().text,
			Impure() != null,
			`object`()?.analyse(scope)
		)
	is TinyScriptParser.AnonymousFunctionCallExpressionContext -> {
		val expression = expression().analyse(scope)
		val signatureIsImpure = Impure() != null
		val argumentsObjectExpression = `object`()?.analyse(scope)
		val argumentsObject = argumentsObjectExpression?.type

		val functionType = expression.type
		if (!(functionType is FunctionType &&
				functionType.isImpure == signatureIsImpure &&
				if (functionType.params != null) {
					argumentsObject != null
						&& functionType.params.accepts(argumentsObject)
				} else {
					argumentsObject == null
				}))
			throw InvalidAnonymousFunctionCallException()

		AnonymousFunctionCallExpression(
			expression,
			signatureIsImpure,
			argumentsObjectExpression,
			functionType.returnType
		)
	}
	is TinyScriptParser.InfixOperatorCallExpressionContext -> {
		val lhsExpression = lhs.analyse(scope)
		val operatorSymbol: String = OperatorSymbol().text
		val operatorIsImpure = Impure() != null
		val rhsExpression = rhs.analyse(scope)

		val valueResult = scope.findOperator(
			lhsExpression.type,
			operatorSymbol,
			operatorIsImpure,
			rhsExpression.type
		)
			?: throw OperatorSignatureNotFoundException(operatorSymbol)

		OperatorCallExpression(
			lhsExpression,
			operatorSymbol,
			operatorIsImpure,
			rhsExpression,
			valueResult
		)
	}
	is TinyScriptParser.ObjectExpressionContext ->
		`object`().analyse(scope)
	is TinyScriptParser.AnonymousFunctionExpressionContext -> {
		val isImpure = Impure() != null
		val paramsObjectTypeExpression = objectType()?.analyse(scope)
		val functionScope = if (paramsObjectTypeExpression != null) {
			FunctionParamsScope(scope, paramsObjectTypeExpression.type)
		} else scope

		val returnExpression = expression().analyse(functionScope)
		if (!isImpure && returnExpression.isImpure)
			throw PureFunctionWithImpureExpressionException()

		AnonymousFunctionExpression(
			isImpure,
			paramsObjectTypeExpression,
			returnExpression
		)
	}
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

fun analyseNameReferenceExpression(
	scope: Scope,
	lhsExpression: Expression?,
	name: String,
	signatureIsImpure: Boolean,
	argumentsObjectExpression: ObjectExpression?
): Expression {
	if (lhsExpression != null && !signatureIsImpure && argumentsObjectExpression == null) {
		val type = lhsExpression.type
		if (type is ObjectType) {
			val fieldType = type.fieldMap[name]
			if (fieldType != null) {
				return ObjectFieldRefExpression(lhsExpression, name, type)
			}
		}
	}

	val valueResult = scope.findNameFunction(
		lhsExpression?.type,
		name,
		signatureIsImpure,
		argumentsObjectExpression?.type
	)
		?: throw NameSignatureNotFoundException(name)

	return NameReferenceExpression(
		lhsExpression,
		name,
		signatureIsImpure,
		argumentsObjectExpression,
		valueResult
	)
}
