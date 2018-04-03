package tinyscript.compiler.core

import tinyscript.compiler.scope.SignatureExpression
import tinyscript.compiler.util.SafeLazy

sealed class Statement

class ValueDeclaration(
	val signatureExpression: SignatureExpression,
	val lazyExpression: SafeLazy<Expression>
) : Statement()

class TypeAliasDeclaration(
	val name: String,
	val lazyTypeExpression: SafeLazy<TypeExpression>
) : Statement()

class ExpressionStatement(
	val expression: Expression
) : Statement()
