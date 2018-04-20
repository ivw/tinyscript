package tinyscript.compiler.scope

import tinyscript.compiler.ast.AnalysisException

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

class TypeResult(scope: Scope, val type: Type) : ScopeResult(scope)

//
//
//

class SignatureEntry<out V>(val signature: Signature, val value: V)

fun NameSignature.couldBeField() = !isImpure && paramsObjectType == null

//
//
//

abstract class Scope(val parentScope: Scope?) {
	val depth: Int = if (parentScope == null) 0 else parentScope.depth + 1

	open fun findValueEntity(signature: Signature): ValueResult? =
		parentScope?.findValueEntity(signature)

	open fun findTypeEntity(name: String): TypeResult? =
		parentScope?.findTypeEntity(name)
}

class DeclarationScope(
	parentScope: Scope?,
	val lazyFieldMap: MutableMap<String, () -> Type> = HashMap(),
	val lazyFunctionMap: MutableList<SignatureEntry<() -> Type>> = ArrayList(),
	val lazyTypeMap: MutableMap<String, () -> Type> = HashMap()
) : Scope(parentScope) {
	override fun findValueEntity(signature: Signature): ValueResult? {
		if (signature is NameSignature && signature.couldBeField()) {
			lazyFieldMap[signature.name]?.let { lazyFieldType ->
				return LocalFieldValueResult(this, lazyFieldType())
			}
		}

		// TODO store functions by name for efficiency
		val functionResults: List<FunctionValueResult> = lazyFunctionMap.mapIndexedNotNull { index, signatureEntry ->
			if (signatureEntry.signature.accepts(signature))
				FunctionValueResult(
					this,
					signatureEntry.value(),
					signatureEntry.signature,
					index
				)
			else null
		}
		if (functionResults.size > 1)
			throw AnalysisException("ambiguous function signatures")
		if (functionResults.size == 1)
			return functionResults[0]

		return super.findValueEntity(signature)
	}

	override fun findTypeEntity(name: String): TypeResult? =
		lazyTypeMap[name]?.let { lazyType -> TypeResult(this, lazyType()) }
			?: parentScope?.findTypeEntity(name)
}

class FunctionScope(
	parentScope: Scope?,
	val paramsObjectType: ObjectType
) : Scope(parentScope) {
	override fun findValueEntity(signature: Signature): ValueResult? {
		if (signature is NameSignature && signature.couldBeField()) {
			paramsObjectType.fieldMap[signature.name]?.let { fieldType ->
				return ParameterValueResult(this, fieldType)
			}
		}

		return super.findValueEntity(signature)
	}

	override fun findTypeEntity(name: String): TypeResult? = null
}
