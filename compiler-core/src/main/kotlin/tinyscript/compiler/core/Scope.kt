package tinyscript.compiler.core

open class Scope(val parentScope: Scope?, val signatures: SignatureCollection) {

	val depth: Int = if (parentScope == null) 0 else parentScope.depth + 1

	fun resolveSymbol(name: String): Symbol? {
		return signatures.getSymbol(name) ?: parentScope?.resolveSymbol(name)
	}

	fun resolveOperator(name: String, lhsType: FinalType?, rhsType: FinalType): Operator? {
		return signatures.getOperator(name, lhsType, rhsType)
				?: parentScope?.resolveOperator(name, lhsType, rhsType)
	}

	fun resolveMethod(name: String, arguments: ObjectType): Method? {
		return signatures.getMethod(name, arguments)
				?: parentScope?.resolveMethod(name, arguments)
	}
}

class ObjectScope(parentScope: Scope?, val objectType: ObjectType) : Scope(parentScope, objectType.signatures) {
	companion object {
		fun resolveObjectScope(scope: Scope): ObjectScope? {
			if (scope is ObjectScope) return scope

			return scope.parentScope?.let { resolveObjectScope(it) }
		}
	}
}

class FunctionScope(parentScope: Scope?, params: ObjectType) : Scope(
		Scope(parentScope, params.signatures),
		SignatureCollection()
)
