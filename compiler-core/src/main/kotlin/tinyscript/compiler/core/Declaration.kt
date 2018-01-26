package tinyscript.compiler.core

import tinyscript.compiler.util.Deferred

abstract class Declaration

class TypeDeclaration(
	val name: String,
	val deferredType: Deferred<Type>
)

class ConcreteDeclaration(
	val signature: Signature,
	val type: Type?,
	val deferredExpression: Deferred<Expression>
) : Declaration()

abstract class Signature

class SymbolSignature(
	val name: String,
	val isImpure: Boolean
) : Signature()

class FunctionSignature(
	val name: String,
	val paramsObjectType: ObjectType,
	val isImpure: Boolean
) : Signature()
