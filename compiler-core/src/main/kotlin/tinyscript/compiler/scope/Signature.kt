package tinyscript.compiler.scope

sealed class Signature {
	// canHaveMutableInput
	abstract val isImpure: Boolean

	abstract val canHaveMutableOutput: Boolean
}

class NameSignature(
	val lhsType: Type?,
	val name: String,
	override val isImpure: Boolean,
	val paramsObjectType: ObjectType?
) : Signature() {
	override val canHaveMutableOutput: Boolean get() = isImpure
}

class ConstructorSignature(
	val name: String,
	val paramsObjectType: ObjectType?
) : Signature() {
	override val isImpure: Boolean get() = false
	override val canHaveMutableOutput: Boolean get() = true
}

class OperatorSignature(
	val lhsType: Type?,
	val operatorSymbol: String,
	override val isImpure: Boolean,
	val rhsType: Type
) : Signature() {
	override val canHaveMutableOutput: Boolean get() = isImpure
}
