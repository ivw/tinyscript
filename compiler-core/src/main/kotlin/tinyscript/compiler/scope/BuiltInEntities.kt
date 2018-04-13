package tinyscript.compiler.scope

val builtInEntities: EntityCollection = MutableEntityCollection().apply {
	valueEntities.add(ValueEntity(
		NameSignature(null, "println", true, ObjectType(emptyMap())),
		{ AnyType }
	))

	typeEntities.add(TypeEntity("Int", { IntType() }))
}
