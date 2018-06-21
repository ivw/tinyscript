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

	open fun findConstructor(
		name: String,
		paramsObjectType: ObjectType?
	): ValueResult? =
		parentScope?.findConstructor(name, paramsObjectType)

	open fun findOperator(
		lhsType: Type?,
		operatorSymbol: String,
		isImpure: Boolean,
		rhsType: Type
	): ValueResult? =
		parentScope?.findOperator(lhsType, operatorSymbol, isImpure, rhsType)

	open fun findType(name: String, isMutable: Boolean): TypeResult? =
		parentScope?.findType(name, isMutable)
}

class LazyScope(
	parentScope: Scope?,
	private val lazyNameSignatureMap: SignatureMap<NameSignature, () -> Type> = SignatureMap(),
	private val lazyConstructorSignatureMap: SignatureMap<ConstructorSignature, () -> Type> = SignatureMap(),
	private val lazyOperatorSignatureMap: SignatureMap<OperatorSignature, () -> Type> = SignatureMap(),
	private val lazyTypeMap: TypeSignatureMap<() -> Type> = TypeSignatureMap()
) : Scope(parentScope) {
	fun addFunction(signature: Signature, lazyType: () -> Type) {
		when (signature) {
			is NameSignature -> lazyNameSignatureMap.add(signature, lazyType)
			is ConstructorSignature -> lazyConstructorSignatureMap.add(signature, lazyType)
			is OperatorSignature -> lazyOperatorSignatureMap.add(signature, lazyType)
		}
	}

	fun addType(name: String, isMutable: Boolean, value: () -> Type) {
		lazyTypeMap.add(name, isMutable, value)
	}

	override fun findNameFunction(
		lhsType: Type?,
		name: String,
		isImpure: Boolean,
		paramsObjectType: ObjectType?
	): ValueResult? =
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

	override fun findConstructor(name: String, paramsObjectType: ObjectType?): ValueResult? =
		lazyConstructorSignatureMap.get { signature ->
			signature.name == name &&
				if (signature.paramsObjectType != null) {
					paramsObjectType != null && signature.paramsObjectType.accepts(paramsObjectType)
				} else {
					paramsObjectType == null
				}
		}?.let { FunctionValueResult(this, it.value(), it.signature, it.index) }
			?: super.findConstructor(name, paramsObjectType)

	override fun findOperator(
		lhsType: Type?,
		operatorSymbol: String,
		isImpure: Boolean,
		rhsType: Type
	): ValueResult? =
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

	override fun findType(name: String, isMutable: Boolean): TypeResult? =
		lazyTypeMap.get(name, isMutable)?.let { TypeResult(this, it()) }
			?: super.findType(name, isMutable)
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

//			if (thisType is ObjectType) {
//				val fieldType = thisType.fieldMap[name]
//				if (fieldType != null) {
//					return ThisFieldValueResult(this, fieldType)
//				}
//			}
		}

//		// this is a problem because a mutable `this` can bypass PureScope
//		if (lhsType == null) {
//			super.findNameFunction(thisType, name, isImpure, paramsObjectType)?.let {
//				return it // TODO needs special kind of ThisScope ValueResult?
//			}
//		}

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

class BlockScope(parentScope: Scope?, val fieldMap: Map<String, Type>) : Scope(parentScope) {
	override fun findNameFunction(
		lhsType: Type?,
		name: String,
		isImpure: Boolean,
		paramsObjectType: ObjectType?
	): ValueResult? {
		if (lhsType == null && !isImpure && paramsObjectType == null) {
			fieldMap[name]?.let { return BlockFieldValueResult(this, it) }
		}

		return super.findNameFunction(lhsType, name, isImpure, paramsObjectType)
	}
}

class OutsideMutableStateException : RuntimeException(
	"function must have `!` to use mutable values outside function scope"
)

class PureScope(parentScope: Scope?) : Scope(parentScope) {
	override fun findNameFunction(lhsType: Type?, name: String, isImpure: Boolean, paramsObjectType: ObjectType?): ValueResult? {
		val result = super.findNameFunction(lhsType, name, isImpure, paramsObjectType)
		if (result != null && result.type.isMutable)
			throw OutsideMutableStateException()
		return result
	}

	// findConstructor is always allowed because constructors can not be impure

	override fun findOperator(lhsType: Type?, operatorSymbol: String, isImpure: Boolean, rhsType: Type): ValueResult? {
		val result = super.findOperator(lhsType, operatorSymbol, isImpure, rhsType)
		if (result != null && result.type.isMutable)
			throw OutsideMutableStateException()
		return result
	}
}
