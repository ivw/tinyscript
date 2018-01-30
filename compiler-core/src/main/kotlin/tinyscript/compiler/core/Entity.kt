package tinyscript.compiler.core

import tinyscript.compiler.util.Deferred

sealed class Entity

class SignatureEntity(
	val deferredSignature: Deferred<Signature>,
	val deferredType: Deferred<Type>
): Entity()

class TypeEntity(
	val name: String,
	val deferredType: Deferred<Type>
): Entity()
