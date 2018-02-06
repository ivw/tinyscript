package tinyscript.compiler.core

sealed class Declaration

class Initializer(val explicitType: Type?, val expression: Expression) {
	val type get() = explicitType ?: expression.type
}

class NameDeclaration(
	val name: String,
	val isImpure: Boolean,
	val initializer: Initializer
) : Declaration()

class FunctionDeclaration(
	val name: String,
	val paramsObjectType: ObjectType,
	val isImpure: Boolean,
	val initializer: Initializer
) : Declaration()

class TypeAliasDeclaration(
	val name: String,
	val type: Type
) : Declaration()

class NonDeclaration(
	val expression: Expression
) : Declaration()
