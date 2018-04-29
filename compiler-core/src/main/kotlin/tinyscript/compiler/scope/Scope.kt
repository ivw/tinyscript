package tinyscript.compiler.scope

abstract class Scope(val parentScope: Scope?) {
	val depth: Int = if (parentScope == null) 0 else parentScope.depth + 1

	open fun findValue(signature: Signature): ValueResult? =
		parentScope?.findValue(signature)

	open fun findType(name: String): TypeResult? =
		parentScope?.findType(name)
}

class DeclarationScope(
	parentScope: Scope?,
	val lazyFieldMap: MutableMap<String, () -> Type> = HashMap(),
	val lazyFunctionMap: SignatureMap<() -> Type> = SignatureMap(),
	val lazyTypeMap: MutableMap<String, () -> Type> = HashMap()
) : Scope(parentScope) {
	override fun findValue(signature: Signature): ValueResult? {
		if (signature is NameSignature && signature.couldBeField()) {
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
		if (signature is NameSignature && signature.couldBeField()) {
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
