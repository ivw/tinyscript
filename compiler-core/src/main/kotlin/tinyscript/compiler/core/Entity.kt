package tinyscript.compiler.core

import tinyscript.compiler.util.Deferred

sealed class Entity

class SignatureEntity(
	val signature: Signature,
	val deferredExpression: Deferred<Expression>
): Entity()

class TypeEntity(
	val name: String,
	val deferredType: Deferred<Type>
): Entity()
