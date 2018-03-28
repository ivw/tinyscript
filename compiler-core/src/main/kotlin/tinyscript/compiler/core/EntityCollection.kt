package tinyscript.compiler.core

interface EntityCollection {
	val valueEntities: List<ValueEntity>

	val typeEntities: List<TypeEntity>

	fun findValueEntity(signature: Signature): ValueEntity?

	fun findTypeEntity(name: String): TypeEntity?
}

class MutableEntityCollection : EntityCollection {
	override val valueEntities: MutableList<ValueEntity> = ArrayList()

	override val typeEntities: MutableList<TypeEntity> = ArrayList()

	override fun findValueEntity(signature: Signature): ValueEntity? =
		valueEntities.find { it.signature.accepts(signature) }

	override fun findTypeEntity(name: String): TypeEntity? =
		typeEntities.find { it.name == name }
}

object EmptyEntityCollection : EntityCollection {
	override val valueEntities: List<ValueEntity> = emptyList()

	override val typeEntities: List<TypeEntity> = emptyList()

	override fun findValueEntity(signature: Signature): ValueEntity? = null

	override fun findTypeEntity(name: String): TypeEntity? = null
}
