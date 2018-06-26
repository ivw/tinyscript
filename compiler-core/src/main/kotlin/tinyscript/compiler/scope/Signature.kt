package tinyscript.compiler.scope

sealed class Signature {
	// canHaveMutableInput
	abstract val isImpure: Boolean

	abstract val canHaveMutableOutput: Boolean

	abstract fun getFunctionScope(parentScope: Scope): Scope
}

class NameSignature(
	val lhsType: Type?,
	val name: String,
	override val isImpure: Boolean,
	val paramsObjectType: ObjectType?
) : Signature() {
	override val canHaveMutableOutput: Boolean get() = isImpure

	override fun getFunctionScope(parentScope: Scope): Scope =
		if (paramsObjectType != null)
			FunctionParamsScope(parentScope, paramsObjectType)
		else parentScope
}

class ConstructorSignature(
	val name: String,
	val paramsObjectType: ObjectType?
) : Signature() {
	override val isImpure: Boolean get() = false
	override val canHaveMutableOutput: Boolean get() = true

	override fun getFunctionScope(parentScope: Scope): Scope =
		if (paramsObjectType != null)
			FunctionParamsScope(parentScope, paramsObjectType)
		else parentScope
}

class OperatorSignature(
	val lhsType: Type?,
	val operatorSymbol: String,
	override val isImpure: Boolean,
	val rhsType: Type
) : Signature() {
	override val canHaveMutableOutput: Boolean get() = isImpure

	override fun getFunctionScope(parentScope: Scope): Scope =
		OperatorFunctionScope(parentScope, lhsType, rhsType)
}
