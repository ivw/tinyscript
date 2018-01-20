package tinyscript.compiler.core

sealed class Entity

abstract class SignatureEntity(val isInitialized: Boolean) {
	abstract fun getType(): Type
}

class EntityCollection : ArrayList<Entity>()
