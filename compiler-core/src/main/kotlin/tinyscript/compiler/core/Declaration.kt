package tinyscript.compiler.core

sealed class Declaration

abstract class SignatureDeclaration(
	val type: Type?,
	val expression: Expression
): Declaration()

class NameDeclaration(
	val name: String,
	val isImpure: Boolean,
	type: Type?,
	expression: Expression
) : SignatureDeclaration(type, expression)

class FunctionDeclaration(
	val name: String,
	val paramsObjectType: ObjectType,
	val isImpure: Boolean,
	type: Type?,
	expression: Expression
) : SignatureDeclaration(type, expression)

class TypeAliasDeclaration(
	val name: String,
	val type: Type
) : Declaration()

class NonDeclaration(
	val expression: Expression
) : Declaration()
