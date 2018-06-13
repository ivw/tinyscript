package tinyscript.compiler.scope

sealed class Signature {
	abstract val isImpure: Boolean

	abstract val hasMutableInput: Boolean
}

class NameSignature(
	val lhsType: Type?,
	val name: String,
	override val isImpure: Boolean,
	val paramsObjectType: ObjectType?
) : Signature() {
	override val hasMutableInput: Boolean =
		(lhsType != null && lhsType.isMutable)
			|| (paramsObjectType != null && paramsObjectType.isMutable)
}

class OperatorSignature(
	val lhsType: Type?,
	val operatorSymbol: String,
	override val isImpure: Boolean,
	val rhsType: Type
) : Signature() {
	override val hasMutableInput: Boolean =
		(lhsType != null && lhsType.isMutable) || rhsType.isMutable
}
