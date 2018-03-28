package tinyscript.compiler.core

import tinyscript.compiler.util.Lazy

sealed class Statement

class ValueDeclaration(
	val signature: Signature,
	val lazyExpression: Lazy<Expression>
) : Statement()

class TypeAliasDeclaration(
	val name: String,
	val lazyType: Lazy<Type>
) : Statement()

class ExpressionStatement(
	val expression: Expression
) : Statement()
