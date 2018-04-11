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

	override fun toString(): String {
		return "AnyType"
	}
}

class ObjectType(
	val fieldMap: Map<String, Type>
) : Type() {
	override fun accepts(type: Type): Boolean {
		if (type !is ObjectType) return false

		if (type === this) return true

//		if (!type.classes.containsAll(classes)) return false

		return fieldMap.entries.all { entry ->
			val subField = type.fieldMap.get(entry.key)
			subField != null && entry.value.accepts(subField)
		}
	}

	override fun toString(): String {
		return "ObjectType<fieldMap = $fieldMap>"
	}
}

class FunctionType(val params: ObjectType, val returnType: Type) : Type() {
	override fun accepts(type: Type): Boolean {
		if (type !is FunctionType) return false

		// `[d: Dog] -> Dog` accepts `[d: Animal] -> SpecialDog`
		return type.params.accepts(params) && returnType.accepts(type.returnType)
	}

	override fun toString(): String {
		return "FunctionType<$params -> $returnType>"
	}
}

class IntType(
	val minValue: Int = Int.MIN_VALUE,
	val maxValue: Int = Int.MAX_VALUE
) : Type() {
	override fun accepts(type: Type): Boolean {
		if (type !is IntType) return false

		return type.minValue >= minValue && type.maxValue <= maxValue
	}
}

class FloatType(
	val minValue: Double = Double.MIN_VALUE,
	val maxValue: Double = Double.MAX_VALUE
) : Type() {
	override fun accepts(type: Type): Boolean {
		if (type !is FloatType) return false

		return type.minValue >= minValue && type.maxValue <= maxValue
	}
}
