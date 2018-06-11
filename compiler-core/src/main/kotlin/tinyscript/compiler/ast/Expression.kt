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

class ReferenceExpression(
	val callSignatureExpression: Signature, // TODO CallSignatureExpression
	val valueResult: ValueResult
) : Expression() {
	override val type: Type = valueResult.type
}

class FieldRefExpression(
	val name: String,
	val valueResult: ValueResult
) : Expression() {
	override val type: Type = valueResult.type
}

class ObjectFieldRefExpression(
	val expression: Expression,
	val name: String,
	override val type: Type
) : Expression()

class FunctionCallExpression(
	val lhsExpression: Expression?,
	val name: String,
	val argumentsObjectExpression: ObjectExpression,
	val valueResult: ValueResult
) : Expression() {
	override val type: Type = valueResult.type
}

class OperatorCallExpression(
	val lhsExpression: Expression?,
	val operatorSymbol: String,
	val rhsExpression: Expression,
	val valueResult: ValueResult
) : Expression() {
	override val type: Type = valueResult.type
}

class AnonymousFunctionCallExpression(
	val expression: Expression,
	val argumentsObjectExpression: ObjectExpression?,
	override val type: Type
) : Expression()

class AnonymousFunctionExpression(
	val paramsObjectTypeExpression: ObjectTypeExpression?,
	val returnExpression: Expression
) : Expression() {
	override val type: Type = FunctionType(paramsObjectTypeExpression?.type, returnExpression.type)
}

class FieldNotFoundException(val name: String) : RuntimeException(
	"unresolved reference '$name'"
)

class ObjectFieldNotFoundException(val name: String) : RuntimeException(
	"unresolved reference '$name'"
)

class SignatureNotFoundException(val signature: Signature) : RuntimeException(
	"unresolved reference '$signature'"
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
	is TinyScriptParser.FieldRefExpressionContext -> {
		val name = Name().text
		val result = scope.findField(name)
			?: throw FieldNotFoundException(name)
		FieldRefExpression(name, result)
	}
	is TinyScriptParser.ObjectFieldRefExpressionContext -> {
		val expression = expression().analyse(scope)
		val name = Name().text

		val objectType = expression.type as? ObjectType
			?: throw ObjectFieldNotFoundException(name)

		val fieldType = objectType.fieldMap[name]
			?: throw ObjectFieldNotFoundException(name)

		ObjectFieldRefExpression(expression, name, fieldType)
	}
	is TinyScriptParser.FunctionCallExpressionContext -> analyseFunctionCallExpression(
		scope,
		null,
		Name().text,
		`object`().analyse(scope)
	)
	is TinyScriptParser.DotFunctionCallExpressionContext -> analyseFunctionCallExpression(
		scope,
		expression().analyse(scope),
		Name().text,
		`object`().analyse(scope)
	)
	is TinyScriptParser.InfixOperatorCallExpressionContext -> {
		val lhsExpression = lhs.analyse(scope)
		val operatorSymbol: String = OperatorSymbol().text
		val rhsExpression = rhs.analyse(scope)

		val operatorSignature = OperatorSignature(lhsExpression.type, operatorSymbol, rhsExpression.type)
		val valueResult = scope.findFunction(operatorSignature)
			?: throw SignatureNotFoundException(operatorSignature)

		OperatorCallExpression(
			lhsExpression,
			operatorSymbol,
			rhsExpression,
			valueResult
		)
	}
	is TinyScriptParser.AnonymousFunctionCallExpressionContext -> {
		val expression = expression().analyse(scope)
		val argumentsObjectExpression = `object`()?.analyse(scope)
		val argumentsObject = argumentsObjectExpression?.type

		val functionType = expression.type
		if (!(functionType is FunctionType &&
				if (functionType.params != null) {
					argumentsObject != null
						&& functionType.params.accepts(argumentsObject)
				} else {
					argumentsObject == null
				}))
			throw InvalidAnonymousFunctionCallException()

		AnonymousFunctionCallExpression(
			expression,
			argumentsObjectExpression,
			functionType.returnType
		)
	}
	is TinyScriptParser.ObjectExpressionContext ->
		`object`().analyse(scope)
	is TinyScriptParser.AnonymousFunctionExpressionContext -> {
		val paramsObjectTypeExpression = objectType()?.analyse(scope)
		val functionScope = if (paramsObjectTypeExpression != null) {
			FunctionParamsScope(scope, paramsObjectTypeExpression.type)
		} else scope

		val returnExpression = expression().analyse(functionScope)

		AnonymousFunctionExpression(paramsObjectTypeExpression, returnExpression)
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

fun analyseFunctionCallExpression(
	scope: Scope,
	lhsExpression: Expression?,
	name: String,
	argumentsObjectExpression: ObjectExpression
): Expression {
	val signature = FunctionSignature(lhsExpression?.type, name, argumentsObjectExpression.type)
	val result = scope.findFunction(signature)
		?: throw SignatureNotFoundException(signature)

	return FunctionCallExpression(lhsExpression, name, argumentsObjectExpression, result)
}
