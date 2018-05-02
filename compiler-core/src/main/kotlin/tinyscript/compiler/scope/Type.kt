package tinyscript.compiler.scope

sealed class Type {
	/**
	 * @return true if `type` is equal to this, or if it is a subtype of this
	 */
	abstract fun accepts(type: Type): Boolean
}

// AnyType accepts anything; objects, functions, etc.
// Nothing can be done with values of AnyType, not even casting.
object AnyType : Type() {
	override fun accepts(type: Type): Boolean = true

	override fun toString(): String = "AnyType"
}

class NativeType : Type() {
	override fun accepts(type: Type): Boolean = type === this
}

class ObjectType(
	val fieldMap: Map<String, Type>
) : Type() {
	override fun accepts(type: Type): Boolean {
		if (type !is ObjectType) return false

		if (type === this) return true

//		if (!type.classes.containsAll(classes)) return false

		return fieldMap.entries.all { entry ->
			val subField = type.fieldMap[entry.key]
			subField != null && entry.value.accepts(subField)
		}
	}

	override fun toString(): String = "ObjectType<fieldMap = $fieldMap>"
}

class FunctionType(
	val isImpure: Boolean,
	val params: ObjectType?,
	val returnType: Type
) : Type() {
	override fun accepts(type: Type): Boolean {
		if (type !is FunctionType) return false

		// `[d: Dog] -> Dog` accepts `[d: Animal] -> SpecialDog`
		return if (params != null) {
			type.params != null &&
				type.params.accepts(params) && returnType.accepts(type.returnType)
		} else {
			type.params == null
		}
	}

	override fun toString(): String = "FunctionType<$params -> $returnType>"
}
