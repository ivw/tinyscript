package tinyscript.compiler.scope

interface EntityCollection {
	val fieldMap: Map<String, Type>

	val functionEntities: List<FunctionEntity>

	val typeMap: Map<String, TypeEntity>
}

class MutableEntityCollection : EntityCollection {
	override val fieldMap: MutableMap<String, Type> = HashMap()

	override val functionEntities: MutableList<FunctionEntity> = ArrayList()

	override val typeMap: MutableMap<String, Type> = HashMap()
}
