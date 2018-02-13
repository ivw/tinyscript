package tinyscript.compiler.core

import tinyscript.compiler.util.Deferred

val builtInEntities: EntityCollection = MutableEntityCollection().apply {
	functionEntities.add(FunctionEntity(
		"println",
		Deferred { ObjectType(MutableEntityCollection(), emptySet()) },
		true,
		Deferred { AnyType }
	))

	typeEntities.add(TypeEntity("Int", Deferred { IntType() }))
}
