package tinyscript.compiler.ast

import tinyscript.compiler.parser.TinyScriptParser
import tinyscript.compiler.scope.*

sealed class SignatureExpression {
	abstract val signature: Signature
}

class NameSignatureExpression(
	val lhsTypeExpression: TypeExpression?,
	val name: String,
	val isImpure: Boolean,
	val paramsObjectTypeExpression: ObjectTypeExpression?
) : SignatureExpression() {
	override val signature = NameSignature(
		lhsTypeExpression?.type,
		name,
		isImpure,
		paramsObjectTypeExpression?.type
	)
}

class ConstructorSignatureExpression(
	val name: String,
	val paramsObjectTypeExpression: ObjectTypeExpression?
) : SignatureExpression() {
	override val signature = ConstructorSignature(
		name,
		paramsObjectTypeExpression?.type
	)
}

class OperatorSignatureExpression(
	val lhsTypeExpression: TypeExpression?,
	val operatorSymbol: String,
	val isImpure: Boolean,
	val rhsTypeExpression: TypeExpression
) : SignatureExpression() {
	override val signature = OperatorSignature(
		lhsTypeExpression?.type,
		operatorSymbol,
		isImpure,
		rhsTypeExpression.type
	)
}

fun TinyScriptParser.SignatureContext.analyse(scope: Scope): SignatureExpression = when (this) {
	is TinyScriptParser.NameSignatureContext -> NameSignatureExpression(
		typeExpression()?.analyse(scope),
		ValueName().text,
		Impure() != null,
		objectType()?.analyse(scope)
	)
	is TinyScriptParser.ConstructorSignatureContext -> ConstructorSignatureExpression(
		ValueName().text,
		objectType()?.analyse(scope)
	)
	is TinyScriptParser.OperatorSignatureContext -> OperatorSignatureExpression(
		lhs?.analyse(scope),
		OperatorSymbol().text,
		Impure() != null,
		rhs.analyse(scope)
	)
	else -> throw RuntimeException("unknown signature class")
}
