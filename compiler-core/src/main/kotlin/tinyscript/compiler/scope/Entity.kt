package tinyscript.compiler.scope

sealed class Entity


abstract class ValueEntity(
	val signature: Signature
) : Entity() {
	abstract val type: Type
}

class BuiltInValueEntity(
	signature: Signature,
	override val type: Type
) : ValueEntity(signature)

class ParameterValueEntity(
	signature: Signature,
	override val type: Type
) : ValueEntity(signature)


abstract class TypeEntity(
	val name: String
) : Entity() {
	abstract val type: Type
}

class BuiltInTypeEntity(
	name: String,
	override val type: Type
) : TypeEntity(name)
