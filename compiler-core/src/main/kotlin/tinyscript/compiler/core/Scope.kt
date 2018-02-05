package tinyscript.compiler.core

class Scope(
	val parentScope: Scope?,
	val entityCollection: EntityCollection
) {
	val depth: Int = if (parentScope == null) 0 else parentScope.depth + 1

	fun findNameEntity(name: String, isImpure: Boolean): NameEntity? =
		entityCollection.findNameEntity(name, isImpure)
			?: parentScope?.findNameEntity(name, isImpure)

	fun findFunctionEntity(
		name: String, paramsObjectType: ObjectType, isImpure: Boolean
	): FunctionEntity? =
		entityCollection.findFunctionEntity(name, paramsObjectType, isImpure)
			?: parentScope?.findFunctionEntity(name, paramsObjectType, isImpure)

	fun findTypeEntity(name: String): TypeEntity? =
		entityCollection.findTypeEntity(name)
			?: parentScope?.findTypeEntity(name)
}
