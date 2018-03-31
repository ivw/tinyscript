package tinyscript.compiler.core

sealed class Entity

class ValueEntity(
	val signature: Signature,
	val getType: () -> Type
) : Entity()

class TypeEntity(
	val name: String,
	val getType: () -> Type
) : Entity()
