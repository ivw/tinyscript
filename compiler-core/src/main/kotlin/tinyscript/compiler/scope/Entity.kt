package tinyscript.compiler.scope

sealed class Entity


abstract class FunctionEntity(
	val signature: Signature
) : Entity() {
	abstract val type: Type
}

class BuiltInFunctionEntity(
	signature: Signature,
	override val type: Type
) : FunctionEntity(signature)


abstract class TypeEntity(
	val name: String
) : Entity() {
	abstract val type: Type
}

class BuiltInTypeEntity(
	name: String,
	override val type: Type
) : TypeEntity(name)
