package tinyscript.compiler.core

sealed class Statement {
	abstract fun finalize()
}

class ImperativeStatement(
	val name: String?,
	val expression: Expression
) : Statement() {
	override fun finalize() {
		// TODO
	}
}

class FunctionDeclaration(
	val signatureExpression: SignatureExpression,
	val expression: Expression
) : Statement() {
	override fun finalize() {
		// TODO
	}
}

class TypeAliasDeclaration(
	val name: String,
	val typeExpression: TypeExpression
) : Statement() {
	override fun finalize() {
		// TODO
	}
}
