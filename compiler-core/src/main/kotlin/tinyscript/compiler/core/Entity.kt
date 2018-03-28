package tinyscript.compiler.core

import tinyscript.compiler.util.Lazy

sealed class Signature {
	abstract fun accepts(signature: Signature): Boolean
}

class NameSignature(
	val name: String,
	val isImpure: Boolean,
	val lazyParamsObjectType: Lazy<ObjectType>?
) : Signature() {
	override fun accepts(signature: Signature): Boolean =
		signature is NameSignature &&
			signature.name == name &&
			signature.isImpure == isImpure &&
			if (lazyParamsObjectType != null) {
				signature.lazyParamsObjectType != null
					&& lazyParamsObjectType.get().accepts(signature.lazyParamsObjectType.get())
			} else {
				signature.lazyParamsObjectType == null
			}
}

sealed class Entity

class ValueEntity(
	val signature: Signature,
	val lazyType: Lazy<Type>
) : Entity()

class TypeEntity(
	val name: String,
	val lazyType: Lazy<Type>
) : Entity()
