package tinyscript.compiler.core

interface EntityCollection {
	val nameEntities: List<NameEntity>
	val functionEntities: List<FunctionEntity>
	val typeEntities: List<TypeEntity>

	fun findNameEntity(name: String, isImpure: Boolean): NameEntity?

	fun findFunctionEntity(
		name: String, paramsObjectType: ObjectType, isImpure: Boolean
	): FunctionEntity?

	fun findTypeEntity(name: String): TypeEntity?
}

class MutableEntityCollection : EntityCollection {
	override val nameEntities: MutableList<NameEntity> = ArrayList()
	override val functionEntities: MutableList<FunctionEntity> = ArrayList()
	override val typeEntities: MutableList<TypeEntity> = ArrayList()

	override fun findNameEntity(name: String, isImpure: Boolean): NameEntity? =
		nameEntities.find {
			it.name == name
				&& it.isImpure == isImpure
		}

	override fun findFunctionEntity(
		name: String, paramsObjectType: ObjectType, isImpure: Boolean
	): FunctionEntity? =
		functionEntities.find {
			it.name == name
				&& it.isImpure == isImpure
				&& it.lazyParamsObjectType.get().accepts(paramsObjectType)
		}

	override fun findTypeEntity(name: String): TypeEntity? =
		typeEntities.find { it.name == name }
}

object EmptyEntityCollection : EntityCollection {
	override val nameEntities: List<NameEntity> = emptyList()
	override val functionEntities: List<FunctionEntity> = emptyList()
	override val typeEntities: List<TypeEntity> = emptyList()

	override fun findNameEntity(name: String, isImpure: Boolean): NameEntity? = null

	override fun findFunctionEntity(name: String, paramsObjectType: ObjectType, isImpure: Boolean): FunctionEntity? = null

	override fun findTypeEntity(name: String): TypeEntity? = null
}
