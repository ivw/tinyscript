package tinyscript.compiler.scope

sealed class Signature {
	abstract val isImpure: Boolean

	abstract fun accepts(signature: Signature): Boolean
}

class NameSignature(
	val lhsType: Type?,
	val name: String,
	override val isImpure: Boolean,
	val paramsObjectType: ObjectType?
) : Signature() {
	override fun accepts(signature: Signature): Boolean =
		signature is NameSignature &&
			if (lhsType != null) {
				signature.lhsType != null
					&& lhsType.accepts(signature.lhsType)
			} else {
				signature.lhsType == null
			} &&
			name == signature.name &&
			isImpure == signature.isImpure &&
			if (paramsObjectType != null) {
				signature.paramsObjectType != null
					&& paramsObjectType.accepts(signature.paramsObjectType)
			} else {
				signature.paramsObjectType == null
			}
}

fun NameSignature.couldBeField() = !isImpure && paramsObjectType == null

fun NameSignature.couldBeLocalField() = lhsType == null && couldBeField()

class OperatorSignature(
	val lhsType: Type?,
	val operatorSymbol: String,
	override val isImpure: Boolean,
	val rhsType: Type
) : Signature() {
	override fun accepts(signature: Signature): Boolean =
		signature is OperatorSignature &&
			if (lhsType != null) {
				signature.lhsType != null
					&& lhsType.accepts(signature.lhsType)
			} else {
				signature.lhsType == null
			} &&
			operatorSymbol == signature.operatorSymbol &&
			isImpure == signature.isImpure &&
			rhsType.accepts(signature.rhsType)
}
