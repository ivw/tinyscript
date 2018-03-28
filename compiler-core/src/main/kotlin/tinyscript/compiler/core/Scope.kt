package tinyscript.compiler.core

class Scope(
	val parentScope: Scope?,
	val entityCollection: EntityCollection
) {
	val depth: Int = if (parentScope == null) 0 else parentScope.depth + 1

	fun findValueEntity(signature: Signature): ValueEntity? =
		entityCollection.findValueEntity(signature)
			?: parentScope?.findValueEntity(signature)

	fun findTypeEntity(name: String): TypeEntity? =
		entityCollection.findTypeEntity(name)
			?: parentScope?.findTypeEntity(name)
}
