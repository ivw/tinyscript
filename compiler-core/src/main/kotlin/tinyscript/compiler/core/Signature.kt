package tinyscript.compiler.core

import tinyscript.compiler.util.Lazy

sealed class Signature {
	abstract fun accepts(signature: Signature): Boolean
}

class NameSignature(
	val name: String,
	val isImpure: Boolean,
	val lazyParamsObjectType: Lazy<ObjectType>?
) : Signature() {
	override fun accepts(signature: Signature): Boolean =
		signature is NameSignature &&
			name == signature.name &&
			isImpure == signature.isImpure &&
			if (lazyParamsObjectType != null) {
				signature.lazyParamsObjectType != null
					&& lazyParamsObjectType.get().accepts(signature.lazyParamsObjectType.get())
			} else {
				signature.lazyParamsObjectType == null
			}
}

class OperatorSignature(
	val lhsType: Lazy<Type>?,
	val operatorSymbol: String,
	val isImpure: Boolean,
	val rhsType: Lazy<Type>
) : Signature() {
	override fun accepts(signature: Signature): Boolean =
		signature is OperatorSignature &&
			if (lhsType != null) {
				signature.lhsType != null
					&& lhsType.get().accepts(signature.lhsType.get())
			} else {
				signature.lhsType == null
			} &&
			operatorSymbol == signature.operatorSymbol &&
			isImpure == signature.isImpure &&
			rhsType.get().accepts(signature.rhsType.get())
}
