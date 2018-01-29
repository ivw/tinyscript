package tinyscript.compiler.core

import tinyscript.compiler.util.Deferred

abstract class Declaration {
	/**
	 * Make sure that every deferred in this object is finalized.
	 */
	abstract fun finalize()
}

class TypeDeclaration(
	val name: String,
	val deferredType: Deferred<Type>
) : Declaration() {
	override fun finalize() {
		deferredType.get()
	}
}

class ConcreteDeclaration(
	val signature: Signature,
	val type: Type?,
	val deferredExpression: Deferred<Expression>
) : Declaration() {
	override fun finalize() {
		deferredExpression.get()
	}
}

abstract class Signature

class SymbolSignature(
	val name: String,
	val isImpure: Boolean
) : Signature()

class FunctionSignature(
	val name: String,
	val paramsObjectType: ObjectType,
	val isImpure: Boolean
) : Signature()
