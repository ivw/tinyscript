package tinyscript.compiler.core

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

class NullableType(val getNonNullType: () -> Type) : Type() {
	override fun accepts(type: Type): Boolean = if (type is NullableType) {
		getNonNullType().accepts(type.getNonNullType())
	} else {
		getNonNullType().accepts(type)
	}

	override fun toString(): String {
		return "NullableType"
	}
}

class FunctionType(val getFunction:() -> Function) : Type() {
	override fun accepts(type: Type): Boolean {
		if (type !is FunctionType) return false

		return getFunction().accepts(type.getFunction())
	}

	override fun toString(): String {
		return "FunctionType"
	}

	class Function(val params: ObjectType, val returnType: Type) {
		fun accepts(function: Function): Boolean {
			// `[d: Dog] -> Dog` accepts `[d: Animal] -> SpecialDog`
			return function.params.accepts(params) && returnType.accepts(function.returnType)
		}
	}
}

class IntType(val minValue: Int = Int.MIN_VALUE, val maxValue: Int = Int.MAX_VALUE) : Type() {
	override fun accepts(type: Type): Boolean {
		if (type !is IntType) return false

		return type.minValue >= minValue && type.maxValue <= maxValue
	}
}

class FloatType(val minValue: Double = Double.MIN_VALUE, val maxValue: Double = Double.MAX_VALUE) : Type() {
	override fun accepts(type: Type): Boolean {
		if (type !is FloatType) return false

		return type.minValue >= minValue && type.maxValue <= maxValue
	}
}
