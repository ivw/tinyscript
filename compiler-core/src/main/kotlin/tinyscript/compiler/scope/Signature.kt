package tinyscript.compiler.scope

sealed class Signature {
	abstract fun accepts(signature: Signature): Boolean
}

class FieldSignature(val name: String) : Signature() {
	override fun accepts(signature: Signature): Boolean =
		signature is FieldSignature &&
			name == signature.name
}

class FunctionSignature(
	val lhsType: Type?,
	val name: String,
	val paramsObjectType: ObjectType
) : Signature() {
	override fun accepts(signature: Signature): Boolean =
		signature is FunctionSignature &&
			if (lhsType != null) {
				signature.lhsType != null
					&& lhsType.accepts(signature.lhsType)
			} else {
				signature.lhsType == null
			} &&
			name == signature.name &&
			paramsObjectType.accepts(signature.paramsObjectType)
}

class OperatorSignature(
	val lhsType: Type?,
	val operatorSymbol: String,
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
			rhsType.accepts(signature.rhsType)
}
