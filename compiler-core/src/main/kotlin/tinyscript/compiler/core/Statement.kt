package tinyscript.compiler.core

import tinyscript.compiler.util.SafeLazy

sealed class Statement

class RunStatement(
	val name: String?,
	val expression: SafeLazy<Expression>
) : Statement()

class FunctionDeclaration(
	val signatureExpression: SignatureExpression,
	val lazyExpression: SafeLazy<Expression>
) : Statement()

class TypeAliasDeclaration(
	val name: String,
	val lazyTypeExpression: SafeLazy<TypeExpression>
) : Statement()
