package tinyscript.compiler.scope

abstract class Scope(val parentScope: Scope?) {
	val depth: Int = if (parentScope == null) 0 else parentScope.depth + 1

	open fun findField(name: String): ValueResult? =
		parentScope?.findField(name)

	open fun findLhsParamlessFunction(lhsType: Type, name: String): ValueResult? =
		parentScope?.findLhsParamlessFunction(lhsType, name)

	open fun findFunction(signature: Signature): ValueResult? =
		parentScope?.findFunction(signature)

	open fun findType(name: String): TypeResult? =
		parentScope?.findType(name)
}

class SimpleScope(
	parentScope: Scope?,
	val fieldMap: MutableMap<String, Type> = HashMap(),
	val lhsParamlessFunctionsMap: SignatureMap<LhsParamlessFunctionSignature, Type> = SignatureMap(),
	val functionMap: SignatureMap<(Signature) -> Type> = SignatureMap(),
	val typeMap: MutableMap<String, Type> = HashMap()
) : Scope(parentScope) {
	override fun findField(name: String): ValueResult? =
		fieldMap[name]?.let { LocalFieldValueResult(this, it) }
			?: super.findField(name)

	override fun findLhsParamlessFunction(lhsType: Type, name: String): ValueResult? =
		lhsParamlessFunctionsMap.get { it.lhsType.accepts(lhsType) && it.name == name }
			?: super.findLhsParamlessFunction(lhsType, name)

	override fun findFunction(signature: Signature): ValueResult? =
		functionMap.get(signature)?.let {
			FunctionValueResult(
				this,
				it.value(signature),
				it.signature,
				it.index
			)
		} ?: super.findFunction(signature)

	override fun findType(name: String): TypeResult? =
		typeMap[name]?.let { TypeResult(this, it) }
			?: super.findType(name)
}

class LazyScope(
	parentScope: Scope?,
	val lazyFieldMap: MutableMap<String, () -> Type> = HashMap(),
	val lazyFunctionMap: SignatureMap<() -> Type> = SignatureMap(),
	val lazyTypeMap: MutableMap<String, () -> Type> = HashMap()
) : Scope(parentScope) {
	override fun findField(name: String): ValueResult? =
		lazyFieldMap[name]?.let { LocalFieldValueResult(this, it()) }
			?: super.findField(name)

	override fun findFunction(signature: Signature): ValueResult? =
		lazyFunctionMap.get(signature)?.let {
			FunctionValueResult(
				this,
				it.value(),
				it.signature,
				it.index
			)
		} ?: super.findFunction(signature)

	override fun findType(name: String): TypeResult? =
		lazyTypeMap[name]?.let { TypeResult(this, it()) }
			?: super.findType(name)
}

class ThisScope(parentScope: Scope?, val thisType: Type) : Scope(parentScope) {
	override fun findField(name: String): ValueResult? {
		if (thisType is ObjectType) {
			thisType.fieldMap[name]?.let { fieldType ->
				return ThisFieldValueResult(this, fieldType)
			}
		}

		if (name == "this") {
			return ThisValueResult(this, thisType)
		}

		return super.findField(name)
	}
}

class FunctionParamsScope(parentScope: Scope?, val paramsObjectType: ObjectType) : Scope(parentScope) {
	override fun findField(name: String): ValueResult? =
		paramsObjectType.fieldMap[name]?.let { ParameterValueResult(this, it) }
			?: super.findField(name)
}

class OperatorFunctionScope(
	parentScope: Scope?,
	val lhsType: Type?,
	val rhsType: Type
) : Scope(parentScope) {
	override fun findField(name: String): ValueResult? {
		if (name == "left" && lhsType != null) {
			return OperatorLhsValueResult(this, lhsType)
		}

		if (name == "right") {
			return OperatorRhsValueResult(this, rhsType)
		}

		return super.findField(name)
	}
}
