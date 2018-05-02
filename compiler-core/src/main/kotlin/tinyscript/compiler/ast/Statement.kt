package tinyscript.compiler.ast

import tinyscript.compiler.scope.NativeType

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

class NativeDeclaration(
	val signatureExpression: SignatureExpression,
	val typeExpression: TypeExpression
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

class NativeTypeDeclaration(val name: String, val nativeType: NativeType) : Statement() {
	override fun finalize() = Unit
}
