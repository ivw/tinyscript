package tinyscript.compiler.core

sealed class Statement

class ImperativeStatement(
	val name: String?,
	val expression: Expression
) : Statement()

class FunctionDeclaration(
	val signatureExpression: SignatureExpression,
	val expression: Expression
) : Statement()

class TypeAliasDeclaration(
	val name: String,
	val typeExpression: TypeExpression
) : Statement()
