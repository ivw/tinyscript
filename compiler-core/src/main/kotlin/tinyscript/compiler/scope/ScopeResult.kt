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

class TypeResult(scope: Scope, val type: Type) : ScopeResult(scope)