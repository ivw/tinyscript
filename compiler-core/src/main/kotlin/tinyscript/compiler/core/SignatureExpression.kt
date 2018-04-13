package tinyscript.compiler.core

import tinyscript.compiler.core.parser.TinyScriptParser
import tinyscript.compiler.scope.*

sealed class SignatureExpression {
	abstract val signature: Signature
}

class NameSignatureExpression(
	val lhsTypeExpression: TypeExpression?,
	val name: String,
	val isImpure: Boolean,
	val paramsObjectType: ObjectTypeExpression?
) : SignatureExpression() {
	override val signature = NameSignature(
		lhsTypeExpression?.type,
		name,
		isImpure,
		paramsObjectType?.type
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
		Name().text,
		Impure() != null,
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
