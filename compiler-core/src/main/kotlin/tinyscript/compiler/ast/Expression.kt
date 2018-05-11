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

class StringExpression(val value: String) : Expression() {
	override val type = stringType
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
	val expression: Expression?,
	val name: String,
	override val isImpure: Boolean,
	val argumentsObjectExpression: ObjectExpression?,
	val valueResult: ValueResult
) : Expression() {
	override val type: Type = valueResult.type
}

class ObjectFieldReference(
	val expression: Expression,
	val name: String,
	override val type: Type
) : Expression() {
	override val isImpure: Boolean get() = false
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

class AnonymousFunctionExpression(
	val isFunctionImpure: Boolean,
	val paramsObjectTypeExpression: ObjectTypeExpression?,
	val returnExpression: Expression
) : Expression() {
	override val type: Type = FunctionType(isImpure, paramsObjectTypeExpression?.type, returnExpression.type)

	override val isImpure: Boolean get() = false
}

class NameSignatureNotFoundException(val nameSignature: NameSignature) : RuntimeException(
	"unresolved reference '${nameSignature.name}'"
)

class OperatorSignatureNotFoundException(val operatorSignature: OperatorSignature) : RuntimeException(
	"unresolved reference '${operatorSignature.operatorSymbol}'"
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
	is TinyScriptParser.NameReferenceExpressionContext ->
		analyseNameReferenceExpression(
			scope,
			null,
			Name().text,
			Impure() != null,
			`object`()?.analyse(scope)
		)
	is TinyScriptParser.DotNameReferenceExpressionContext ->
		analyseNameReferenceExpression(
			scope,
			expression().analyse(scope),
			Name().text,
			Impure() != null,
			`object`()?.analyse(scope)
		)
	is TinyScriptParser.InfixOperatorCallExpressionContext -> {
		val lhsExpression = lhs.analyse(scope)
		val operatorSymbol: String = OperatorSymbol().text
		val isImpure: Boolean = Impure() != null
		val rhsExpression = rhs.analyse(scope)

		val operatorSignature = OperatorSignature(
			lhsExpression.type,
			operatorSymbol,
			isImpure,
			rhsExpression.type
		)
		val valueResult = scope.findValue(operatorSignature)
			?: throw OperatorSignatureNotFoundException(operatorSignature)

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
	is TinyScriptParser.AnonymousFunctionExpressionContext -> {
		val paramsObjectTypeExpression = objectType()?.analyse(scope)
		val functionScope = if (paramsObjectTypeExpression != null) {
			FunctionParamsScope(scope, paramsObjectTypeExpression.type)
		} else scope

		AnonymousFunctionExpression(
			Impure() != null,
			paramsObjectTypeExpression,
			expression().analyse(functionScope)
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
	expression: Expression?,
	name: String,
	isImpure: Boolean,
	argumentsObjectExpression: ObjectExpression?
): Expression {
	val nameSignature = NameSignature(
		expression?.type,
		name,
		isImpure,
		argumentsObjectExpression?.type
	)

	if (expression != null && nameSignature.couldBeField()) {
		val type = expression.type
		if (type is ObjectType) {
			val fieldType = type.fieldMap[name]
			if (fieldType != null) {
				return ObjectFieldReference(expression, name, type)
			}
		}
	}

	val valueResult = scope.findValue(nameSignature)
		?: throw NameSignatureNotFoundException(nameSignature)

	return NameReferenceExpression(
		expression,
		name,
		isImpure,
		argumentsObjectExpression,
		valueResult
	)
}
