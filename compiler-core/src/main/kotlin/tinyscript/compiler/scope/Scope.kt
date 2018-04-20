package tinyscript.compiler.scope

sealed class ScopeResult(val scope: Scope)

sealed class ValueResult(scope: Scope, val type: Type) : ScopeResult(scope)

class LocalFieldValueResult(scope: Scope, type: Type) : ValueResult(scope, type)

class FunctionValueResult(
	scope: Scope,
	type: Type,
	val signature: Signature,
	val signatureIndex: Int
) : ValueResult(scope, type)

class ParameterValueResult(scope: Scope, type: Type) : ValueResult(scope, type)

class TypeResult(scope: Scope) : ScopeResult(scope)

//
//
//

class SignatureEntry<out V>(val signature: Signature, val value: V)

fun NameSignature.couldBeField() = !isImpure && paramsObjectType == null

//
//
//

sealed class Scope(val parentScope: Scope?) {
	val depth: Int = if (parentScope == null) 0 else parentScope.depth + 1

	open fun findValueEntity(signature: Signature): ValueResult? =
		parentScope?.findValueEntity(signature)

	open fun findTypeEntity(name: String): TypeResult? =
		parentScope?.findTypeEntity(name)
}

class DeclarationScope(
	parentScope: Scope?,
	val lazyFieldMap: Map<String, () -> Type>,
	val lazyFunctionMap: List<SignatureEntry<() -> Type>>,
	val lazyTypeMap: Map<String, () -> Type>
) : Scope(parentScope) {
	override fun findValueEntity(signature: Signature): ValueResult? {
		if (signature is NameSignature && !signature.couldBeField()) {
			lazyFieldMap[signature.name]?.let {lazyFieldType ->
				return LocalFieldValueResult(this, lazyFieldType())
			}
		}

		// TODO

		return super.findValueEntity(signature)
	}

	override fun findTypeEntity(name: String): TypeResult? =
		entityCollection.findTypeEntity(name)
			?: parentScope?.findTypeEntity(name)
}

class FunctionScope(
	parentScope: Scope?,
	val paramsObjectType: ObjectType
) : Scope(parentScope) {
	override fun findValueEntity(signature: Signature): ValueResult? {
		if (signature is NameSignature && !signature.couldBeField()) {
			paramsObjectType.fieldMap[signature.name]?.let { fieldType ->
				return ParameterValueResult(this, fieldType)
			}
		}

		return super.findValueEntity(signature)
	}

	override fun findTypeEntity(name: String): TypeResult? = null
}
