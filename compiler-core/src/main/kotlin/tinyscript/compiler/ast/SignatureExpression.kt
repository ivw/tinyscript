package tinyscript.compiler.ast

import tinyscript.compiler.parser.TinyScriptParser
import tinyscript.compiler.scope.*

sealed class SignatureExpression {
	abstract val signature: Signature
}

class FieldSignatureExpression(
	val name: String,
) : SignatureExpression() {
	override val signature = FieldSignature(name)
}

class FunctionSignatureExpression(
	val lhsTypeExpression: TypeExpression?,
	val name: String,
	val paramsObjectTypeExpression: ObjectTypeExpression
) : SignatureExpression() {
	override val signature = FunctionSignature(
		lhsTypeExpression?.type,
		name,
		paramsObjectTypeExpression.type
	)
}

class OperatorSignatureExpression(
	val lhsTypeExpression: TypeExpression?,
	val operatorSymbol: String,
	val rhsTypeExpression: TypeExpression
) : SignatureExpression() {
	override val signature = OperatorSignature(
		lhsTypeExpression?.type,
		operatorSymbol,
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
