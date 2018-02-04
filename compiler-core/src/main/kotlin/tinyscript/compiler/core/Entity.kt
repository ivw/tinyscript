package tinyscript.compiler.core

import tinyscript.compiler.util.Deferred

sealed class Entity

abstract class SignatureEntity(
	val deferredType: Deferred<Type>
): Entity()

class NameEntity(
	val name: String,
	val isImpure: Boolean,
	deferredType: Deferred<Type>
): SignatureEntity(deferredType)

class FunctionEntity(
	val name: String,
	val deferredParamsObjectType: Deferred<ObjectType>,
	val isImpure: Boolean,
	deferredType: Deferred<Type>
): SignatureEntity(deferredType)

class TypeEntity(
	val name: String,
	val deferredType: Deferred<Type>
): Entity()
