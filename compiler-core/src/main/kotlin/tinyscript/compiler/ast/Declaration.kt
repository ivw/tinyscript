package tinyscript.compiler.ast

import tinyscript.compiler.scope.AtomicType

sealed class Declaration {
	abstract fun finalize()
}

class ValueDefinition(
	val signatureExpression: SignatureExpression,
	val expression: Expression
) : Declaration() {
	override fun finalize() {
		// TODO
	}
}

class NativeDeclaration(
	val signatureExpression: SignatureExpression,
	val typeExpression: TypeExpression
) : Declaration() {
	override fun finalize() {
		// TODO
	}
}

class TypeAliasDefinition(
	val name: String,
	val typeExpression: TypeExpression
) : Declaration() {
	override fun finalize() {
		// TODO
	}
}

class NativeTypeDeclaration(val name: String, val atomicType: AtomicType) : Declaration() {
	override fun finalize() = Unit
}
