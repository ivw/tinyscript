package tinyscript.compiler.scope

sealed class Signature {
	open val isImpure: Boolean = false

	open val isConstructor: Boolean = false
}

class NameSignature(
	val lhsType: Type?,
	val name: String,
	override val isImpure: Boolean,
	val paramsObjectType: ObjectType?
) : Signature()

class ConstructorSignature(
	val name: String,
	val paramsObjectType: ObjectType?
) : Signature() {
	override val isConstructor: Boolean get() = true
}

class OperatorSignature(
	val lhsType: Type?,
	val operatorSymbol: String,
	override val isImpure: Boolean,
	val rhsType: Type
) : Signature()
