package tinyscript.compiler.core

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

sealed class Signature {
	abstract fun accepts(signature: Signature): Boolean
}

class NameSignature(
	val getLhsType: (() -> Type)?,
	val name: String,
	val isImpure: Boolean,
	val getParamsObjectType: (() -> ObjectType)?
) : Signature() {
	override fun accepts(signature: Signature): Boolean =
		signature is NameSignature &&
			if (getLhsType != null) {
				signature.getLhsType != null
					&& getLhsType.invoke().accepts(signature.getLhsType.invoke())
			} else {
				signature.getLhsType == null
			} &&
			name == signature.name &&
			isImpure == signature.isImpure &&
			if (getParamsObjectType != null) {
				signature.getParamsObjectType != null
					&& getParamsObjectType.invoke().accepts(signature.getParamsObjectType.invoke())
			} else {
				signature.getParamsObjectType == null
			}
}

class OperatorSignature(
	val getLhsType: (() -> Type)?,
	val operatorSymbol: String,
	val isImpure: Boolean,
	val getRhsType: () -> Type
) : Signature() {
	override fun accepts(signature: Signature): Boolean =
		signature is OperatorSignature &&
			if (getLhsType != null) {
				signature.getLhsType != null
					&& getLhsType.invoke().accepts(signature.getLhsType.invoke())
			} else {
				signature.getLhsType == null
			} &&
			operatorSymbol == signature.operatorSymbol &&
			isImpure == signature.isImpure &&
			getRhsType.invoke().accepts(signature.getRhsType.invoke())
}
