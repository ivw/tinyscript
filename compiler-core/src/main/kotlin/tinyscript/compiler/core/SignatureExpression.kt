package tinyscript.compiler.core

import tinyscript.compiler.core.TypeExpression
import tinyscript.compiler.scope.NameSignature
import tinyscript.compiler.scope.ObjectType
import tinyscript.compiler.scope.Signature
import tinyscript.compiler.util.SafeLazy

sealed class SignatureExpression {
	abstract val signature: Signature
}

class NameSignatureExpression(
	val lazyLhsTypeExpression: SafeLazy<TypeExpression>?,
	val name: String,
	val isImpure: Boolean,
	val lazyParamsObjectType: SafeLazy<ObjectType>?
) : SignatureExpression() {
	override val signature = NameSignature(
		lazyLhsTypeExpression?.let {
			{ lazyLhsTypeExpression.get().type }
		},
		name,
		isImpure,
		lazyParamsObjectType?.let {
			{ lazyParamsObjectType.get() }
		}
	)
}
