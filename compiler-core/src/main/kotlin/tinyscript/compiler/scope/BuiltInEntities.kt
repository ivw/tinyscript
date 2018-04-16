package tinyscript.compiler.scope

val builtInEntities: EntityCollection = MutableEntityCollection().apply {
	valueEntities.add(BuiltInValueEntity(
		NameSignature(null, "println", true, ObjectType(emptyMap())),
		AnyType
	))

	typeEntities.add(BuiltInTypeEntity("Int", IntType()))
}
