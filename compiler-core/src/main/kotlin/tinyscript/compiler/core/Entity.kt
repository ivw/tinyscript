package tinyscript.compiler.core

import tinyscript.compiler.util.Deferred

sealed class Entity

class NameEntity(
	val name: String,
	val isImpure: Boolean,
	val deferredType: Deferred<Type>
): Entity()

class FunctionEntity(
	val name: String,
	val deferredParamsObjectType: Deferred<ObjectType>,
	val isImpure: Boolean,
	val deferredType: Deferred<Type>
): Entity()

class TypeEntity(
	val name: String,
	val deferredType: Deferred<Type>
): Entity()
