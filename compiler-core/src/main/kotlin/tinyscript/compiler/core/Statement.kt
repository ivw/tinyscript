package tinyscript.compiler.core

sealed class Statement

class NameDeclaration(
	val name: String,
	val isImpure: Boolean,
	val expression: Expression
) : Statement()

class FunctionDeclaration(
	val name: String,
	val paramsObjectType: ObjectType,
	val isImpure: Boolean,
	val expression: Expression
) : Statement()

class TypeAliasDeclaration(
	val name: String,
	val type: Type
) : Statement()

class ExpressionStatement(
	val expression: Expression
) : Statement()
