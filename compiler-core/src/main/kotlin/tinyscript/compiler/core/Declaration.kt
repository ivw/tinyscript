package tinyscript.compiler.core

abstract class Declaration

class TypeAliasDeclaration(
	val name: String,
	val type: Type
) : Declaration()

class SignatureDeclaration(
	val signature: Signature,
	val type: Type?,
	val expression: Expression
) : Declaration()

class NonDeclaration(
	val expression: Expression
) : Declaration()

abstract class Signature

class NameSignature(
	val name: String,
	val isImpure: Boolean
) : Signature()

class FunctionSignature(
	val name: String,
	val paramsObjectType: ObjectType,
	val isImpure: Boolean
) : Signature()
