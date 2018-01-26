package tinyscript.compiler.core

sealed class Entity

abstract class SignatureEntity {
	abstract fun getType(): Type
}

class EntityCollection : ArrayList<Entity>()
