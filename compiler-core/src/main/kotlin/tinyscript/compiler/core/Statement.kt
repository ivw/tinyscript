package tinyscript.compiler.core

import tinyscript.compiler.scope.NameSignature
import tinyscript.compiler.util.SafeLazy

sealed class Statement {
	abstract val isRunStatement: Boolean
}

class ValueDeclaration(
	val signatureExpression: SignatureExpression,
	val lazyExpression: SafeLazy<Expression>
) : Statement() {
	override val isRunStatement: Boolean =
		signatureExpression.signature.let {
			it is NameSignature &&
				it.getLhsType == null &&
				!it.isImpure &&
				it.getParamsObjectType == null
		}
}

class TypeAliasDeclaration(
	val name: String,
	val lazyTypeExpression: SafeLazy<TypeExpression>
) : Statement() {
	override val isRunStatement: Boolean = false
}

class ExpressionStatement(
	val expression: Expression
) : Statement() {
	override val isRunStatement: Boolean = true
}
