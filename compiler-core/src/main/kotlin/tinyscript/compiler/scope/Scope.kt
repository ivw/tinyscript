package tinyscript.compiler.scope

abstract class Scope(val parentScope: Scope?) {
	val depth: Int = if (parentScope == null) 0 else parentScope.depth + 1

	open fun findValue(signature: Signature): ValueResult? =
		parentScope?.findValue(signature)

	open fun findType(name: String): TypeResult? =
		parentScope?.findType(name)
}

class SimpleScope(
	parentScope: Scope?,
	val fieldMap: MutableMap<String, Type> = HashMap(),
	val functionMap: SignatureMap<(Signature) -> Type> = SignatureMap(),
	val typeMap: MutableMap<String, Type> = HashMap()
) : Scope(parentScope) {
	override fun findValue(signature: Signature): ValueResult? {
		if (signature is NameSignature && signature.couldBeLocalField()) {
			fieldMap[signature.name]?.let { fieldType ->
				return LocalFieldValueResult(this, fieldType)
			}
		}

		return functionMap.get(signature)?.let {
			FunctionValueResult(
				this,
				it.value(signature),
				it.signature,
				it.index
			)
		} ?: super.findValue(signature)
	}

	override fun findType(name: String): TypeResult? =
		typeMap[name]?.let { type -> TypeResult(this, type) }
			?: super.findType(name)
}

class LazyScope(
	parentScope: Scope?,
	val lazyFieldMap: MutableMap<String, () -> Type> = HashMap(),
	val lazyFunctionMap: SignatureMap<() -> Type> = SignatureMap(),
	val lazyTypeMap: MutableMap<String, () -> Type> = HashMap()
) : Scope(parentScope) {
	override fun findValue(signature: Signature): ValueResult? {
		if (signature is NameSignature && signature.couldBeLocalField()) {
			lazyFieldMap[signature.name]?.let { lazyFieldType ->
				return LocalFieldValueResult(this, lazyFieldType())
			}
		}

		return lazyFunctionMap.get(signature)?.let {
			FunctionValueResult(
				this,
				it.value(),
				it.signature,
				it.index
			)
		} ?: super.findValue(signature)
	}

	override fun findType(name: String): TypeResult? =
		lazyTypeMap[name]?.let { lazyType -> TypeResult(this, lazyType()) }
			?: super.findType(name)
}

class FunctionScope(
	parentScope: Scope?,
	val functionSignature: Signature
) : Scope(parentScope) {
	override fun findValue(signature: Signature): ValueResult? {
		if (signature is NameSignature && signature.couldBeLocalField()) {
			if (functionSignature is NameSignature) {
				if (signature.name == "this" && functionSignature.lhsType != null) {
					return ThisValueResult(this, functionSignature.lhsType)
				}

				if (functionSignature.paramsObjectType != null) {
					functionSignature.paramsObjectType.fieldMap[signature.name]?.let { fieldType ->
						return ParameterValueResult(this, fieldType)
					}
				}
			}

			if (functionSignature is OperatorSignature) {
				if (signature.name == "left" && functionSignature.lhsType != null) {
					return OperatorLhsValueResult(this, functionSignature.lhsType)
				}

				if (signature.name == "right") {
					return OperatorRhsValueResult(this, functionSignature.rhsType)
				}
			}
		}

		return super.findValue(signature)
	}
}
