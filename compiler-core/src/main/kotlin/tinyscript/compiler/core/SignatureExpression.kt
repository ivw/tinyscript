package tinyscript.compiler.core

import tinyscript.compiler.scope.NameSignature
import tinyscript.compiler.scope.ObjectType
import tinyscript.compiler.scope.OperatorSignature
import tinyscript.compiler.scope.Signature

sealed class SignatureExpression {
	abstract val signature: Signature
}

class NameSignatureExpression(
	val lhsTypeExpression: TypeExpression?,
	val name: String,
	val isImpure: Boolean,
	val paramsObjectType: ObjectType?
) : SignatureExpression() {
	override val signature = NameSignature(
		lhsTypeExpression?.type,
		name,
		isImpure,
		paramsObjectType
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
