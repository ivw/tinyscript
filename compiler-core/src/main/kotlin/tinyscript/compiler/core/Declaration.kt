package tinyscript.compiler.core

abstract class Declaration

class TypeDeclaration(
	val name: String,
	val type: Type
) : Declaration()

class ConcreteDeclaration(
	val signature: Signature,
	val type: Type?,
	val expression: Expression
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
