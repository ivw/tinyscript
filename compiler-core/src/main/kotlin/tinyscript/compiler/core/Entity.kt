package tinyscript.compiler.core

import tinyscript.compiler.util.Lazy

sealed class Entity

class ValueEntity(
	val signature: Signature,
	val lazyType: Lazy<Type>
) : Entity()

class TypeEntity(
	val name: String,
	val lazyType: Lazy<Type>
) : Entity()
