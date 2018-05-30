package tinyscript.compiler.ast

import tinyscript.compiler.parser.TinyScriptParser
import tinyscript.compiler.scope.*

sealed class Expression {
	abstract val type: Type

	abstract val mutatesScope: Scope?
}

class AnyExpression : Expression() {
	override val type = AnyType
	override val mutatesScope: Scope? = null
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
	override val mutatesScope: Scope? get() = null
}

class FloatExpression(val value: Double) : Expression() {
	override val type = FloatType(value, value)
	override val mutatesScope: Scope? get() = null
}

class StringExpression(val value: String) : Expression() {
	override val type = stringType
	override val mutatesScope: Scope? get() = null
}

class ObjectExpression(val objectStatements: List<ObjectStatement>) : Expression() {
	override val type = ObjectType(objectStatements.fold(mutableMapOf(), { mutableFieldMap, objectStatement ->
		when (objectStatement) {
			is ObjectFieldDeclaration -> {
				mutableFieldMap[objectStatement.name] = objectStatement.expression.type
			}
			is ObjectInheritStatement -> {
				TODO()
			}
		}
		mutableFieldMap
	}))

	override val isImpure: Boolean =
		objectStatements.any { it.isImpure }
}

class NameReferenceExpression(
	val expression: Expression?,
	val name: String,
	val signatureIsImpure: Boolean,
	val argumentsObjectExpression: ObjectExpression?,
	val valueResult: ValueResult
) : Expression() {
	override val type: Type = valueResult.type

	override val mutatesScope: Scope? =
		if (valueResult.type.hasMutableState && valueResult is LocalFieldValueResult)
			valueResult.scope else null
}

class ObjectFieldReference(
	val expression: Expression,
	val name: String,
	override val type: Type
) : Expression() {
	override val isImpure: Boolean get() = false
}

class AnonymousFunctionCallExpression(
	val expression: Expression,
	val signatureIsImpure: Boolean,
	val argumentsObjectExpression: ObjectExpression?,
	override val type: Type
) : Expression() {
	override val mutatesScope: Scope? = getOutermostScope(
		expression.mutatesScope,
		argumentsObjectExpression?.mutatesScope
	)
}

class OperatorCallExpression(
	val lhsExpression: Expression?,
	val operatorSymbol: String,
	val operatorIsImpure: Boolean,
	val rhsExpression: Expression,
	val valueResult: ValueResult
) : Expression() {
	override val type: Type = valueResult.type

	override val mutatesScope: Scope? = getOutermostScope(
		lhsExpression?.mutatesScope,
		rhsExpression.mutatesScope
	)
}

class AnonymousFunctionExpression(
	val isFunctionImpure: Boolean,
	val paramsObjectTypeExpression: ObjectTypeExpression?,
	val returnExpression: Expression
) : Expression() {
	override val type: Type = FunctionType(isFunctionImpure, paramsObjectTypeExpression?.type, returnExpression.type)

	override val mutatesScope: Scope? get() = null
}

class NameSignatureNotFoundException(val nameSignature: NameSignature) : RuntimeException(
	"unresolved reference '${nameSignature.name}'"
)

class OperatorSignatureNotFoundException(val operatorSignature: OperatorSignature) : RuntimeException(
	"unresolved reference '${operatorSignature.operatorSymbol}'"
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

		val operatorSignature = OperatorSignature(
			lhsExpression.type,
			operatorSymbol,
			operatorIsImpure,
			rhsExpression.type
		)
		val valueResult = scope.findValue(operatorSignature)
			?: throw OperatorSignatureNotFoundException(operatorSignature)

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
	val nameSignature = NameSignature(
		lhsExpression?.type,
		name,
		signatureIsImpure,
		argumentsObjectExpression?.type
	)

	if (lhsExpression != null && nameSignature.couldBeField()) {
		val type = lhsExpression.type
		if (type is ObjectType) {
			val fieldType = type.fieldMap[name]
			if (fieldType != null) {
				return ObjectFieldReference(lhsExpression, name, type)
			}
		}
	}

	val valueResult = scope.findValue(nameSignature)
		?: throw NameSignatureNotFoundException(nameSignature)

	return NameReferenceExpression(
		lhsExpression,
		name,
		signatureIsImpure,
		argumentsObjectExpression,
		valueResult
	)
}
