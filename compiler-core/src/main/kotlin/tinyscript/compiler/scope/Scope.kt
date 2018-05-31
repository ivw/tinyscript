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
		if (signature is FieldSignature) {
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
		if (signature is FieldSignature) {
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

class ThisScope(parentScope: Scope?, val thisType: Type) : Scope(parentScope) {
	override fun findValue(signature: Signature): ValueResult? {
		if (signature is FieldSignature) {
			if (thisType is ObjectType) {
				thisType.fieldMap[signature.name]?.let { fieldType ->
					return ThisFieldValueResult(this, fieldType)
				}
			}

			if (signature.name == "this") {
				return ThisValueResult(this, thisType)
			}
		}

		return super.findValue(signature)
	}
}

class FunctionParamsScope(parentScope: Scope?, val paramsObjectType: ObjectType) : Scope(parentScope) {
	override fun findValue(signature: Signature): ValueResult? {
		if (signature is FieldSignature) {
			paramsObjectType.fieldMap[signature.name]?.let { fieldType ->
				return ParameterValueResult(this, fieldType)
			}
		}

		return super.findValue(signature)
	}
}

class OperatorFunctionScope(
	parentScope: Scope?,
	val lhsType: Type?,
	val rhsType: Type
) : Scope(parentScope) {
	override fun findValue(signature: Signature): ValueResult? {
		if (signature is FieldSignature) {
			if (signature.name == "left" && lhsType != null) {
				return OperatorLhsValueResult(this, lhsType)
			}

			if (signature.name == "right") {
				return OperatorRhsValueResult(this, rhsType)
			}
		}

		return super.findValue(signature)
	}
}
