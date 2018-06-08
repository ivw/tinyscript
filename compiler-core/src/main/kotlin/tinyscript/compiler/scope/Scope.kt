package tinyscript.compiler.scope

abstract class Scope(val parentScope: Scope?) {
	val depth: Int = if (parentScope == null) 0 else parentScope.depth + 1

	open fun findNameFunction(
		lhsType: Type?,
		name: String,
		isImpure: Boolean,
		paramsObjectType: ObjectType?
	): ValueResult? =
		parentScope?.findNameFunction(lhsType, name, isImpure, paramsObjectType)

	open fun findOperator(
		lhsType: Type?,
		operatorSymbol: String,
		isImpure: Boolean,
		rhsType: Type
	): ValueResult? =
		parentScope?.findOperator(lhsType, operatorSymbol, isImpure, rhsType)

	open fun findType(name: String): TypeResult? =
		parentScope?.findType(name)
}

class LazyScope(
	parentScope: Scope?,
	val lazyNameSignatureMap: SignatureMap<NameSignature, () -> Type> = SignatureMap(),
	val lazyOperatorSignatureMap: SignatureMap<OperatorSignature, () -> Type> = SignatureMap(),
	val lazyTypeMap: MutableMap<String, () -> Type> = HashMap()
) : Scope(parentScope) {
	override fun findNameFunction(lhsType: Type?, name: String, isImpure: Boolean, paramsObjectType: ObjectType?): ValueResult? =
		lazyNameSignatureMap.get { signature ->
			if (signature.lhsType != null) {
				lhsType != null && signature.lhsType.accepts(lhsType)
			} else {
				lhsType == null
			} &&
				signature.name == name &&
				signature.isImpure == isImpure &&
				if (signature.paramsObjectType != null) {
					paramsObjectType != null && signature.paramsObjectType.accepts(paramsObjectType)
				} else {
					paramsObjectType == null
				}
		}?.let { FunctionValueResult(this, it.value(), it.signature, it.index) }
			?: super.findNameFunction(lhsType, name, isImpure, paramsObjectType)

	override fun findOperator(lhsType: Type?, operatorSymbol: String, isImpure: Boolean, rhsType: Type): ValueResult? =
		lazyOperatorSignatureMap.get { signature ->
			if (signature.lhsType != null) {
				lhsType != null && signature.lhsType.accepts(lhsType)
			} else {
				lhsType == null
			} &&
				signature.operatorSymbol == operatorSymbol &&
				signature.isImpure == isImpure &&
				signature.rhsType.accepts(rhsType)
		}?.let { FunctionValueResult(this, it.value(), it.signature, it.index) }
			?: super.findOperator(lhsType, operatorSymbol, isImpure, rhsType)

	override fun findType(name: String): TypeResult? =
		lazyTypeMap[name]?.let { TypeResult(this, it()) }
			?: super.findType(name)
}

class ThisScope(parentScope: Scope?, val thisType: Type) : Scope(parentScope) {
	override fun findNameFunction(
		lhsType: Type?,
		name: String,
		isImpure: Boolean,
		paramsObjectType: ObjectType?
	): ValueResult? {
		if (lhsType == null && !isImpure && paramsObjectType == null) {
			if (name == "this") {
				return ThisValueResult(this, thisType)
			}

			// TODO maybe also have to check if thisType is an object type
		}

		if (lhsType == null) {
			super.findNameFunction(thisType, name, isImpure, paramsObjectType)?.let {
				return it
			}
		}

		return super.findNameFunction(lhsType, name, isImpure, paramsObjectType)
	}
}

class FunctionParamsScope(parentScope: Scope?, val scopeParams: ObjectType) : Scope(parentScope) {
	override fun findNameFunction(
		lhsType: Type?,
		name: String,
		isImpure: Boolean,
		paramsObjectType: ObjectType?
	): ValueResult? {
		if (lhsType == null && !isImpure && paramsObjectType == null) {
			scopeParams.fieldMap[name]?.let { return ParameterValueResult(this, it) }
		}

		return super.findNameFunction(lhsType, name, isImpure, paramsObjectType)
	}
}

class OperatorFunctionScope(
	parentScope: Scope?,
	val scopeLhsType: Type?,
	val scopeRhsType: Type
) : Scope(parentScope) {
	override fun findNameFunction(
		lhsType: Type?,
		name: String,
		isImpure: Boolean,
		paramsObjectType: ObjectType?
	): ValueResult? {
		if (lhsType == null && !isImpure && paramsObjectType == null) {
			if (name == "left" && scopeLhsType != null) {
				return OperatorLhsValueResult(this, scopeLhsType)
			}

			if (name == "right") {
				return OperatorRhsValueResult(this, scopeRhsType)
			}
		}

		return super.findNameFunction(lhsType, name, isImpure, paramsObjectType)
	}
}
