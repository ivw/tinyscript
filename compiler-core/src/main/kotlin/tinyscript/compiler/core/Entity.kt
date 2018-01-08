package tinyscript.compiler.core

class Entity(val type: Type, val isInitialized: Boolean)

class EntityCollection : ArrayList<Entity>()
