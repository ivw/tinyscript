package tinyscript.compiler.scope

sealed class Signature {
	abstract val isImpure: Boolean

	abstract val hasMutableInput: Boolean
}

class ParamlessFunctionSignature(
	val name: String,
	override val isImpure: Boolean
) : Signature() {
	override val hasMutableInput: Boolean = false
}

class LhsParamlessFunctionSignature(
	val lhsType: Type,
	val name: String,
	override val isImpure: Boolean
) : Signature() {
	override val hasMutableInput: Boolean = lhsType.hasMutableState
}

class ParamFunctionSignature(
	val name: String,
	override val isImpure: Boolean,
	val paramsObjectType: ObjectType
) : Signature() {
	override val hasMutableInput: Boolean = paramsObjectType.hasMutableState
}

class LhsParamFunctionSignature(
	val lhsType: Type,
	val name: String,
	override val isImpure: Boolean,
	val paramsObjectType: ObjectType
) : Signature() {
	override val hasMutableInput: Boolean =
		lhsType.hasMutableState || paramsObjectType.hasMutableState
}

class PrefixOperatorSignature(
	val operatorSymbol: String,
	override val isImpure: Boolean,
	val rhsType: Type
) : Signature() {
	override val hasMutableInput: Boolean = rhsType.hasMutableState
}

class InfixOperatorSignature(
	val lhsType: Type,
	val operatorSymbol: String,
	override val isImpure: Boolean,
	val rhsType: Type
) : Signature() {
	override val hasMutableInput: Boolean =
		lhsType.hasMutableState || rhsType.hasMutableState
}
