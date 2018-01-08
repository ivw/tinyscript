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
	val entities: EntityCollection = EntityCollection(),
	val classes: MutableSet<ClassType> = HashSet()
) : Type() {
	override fun accepts(type: Type): Boolean {
		if (type !is ObjectType) return false

		// `[ &Foo, bar: Int, abc = 123 ]` accepts `[ &Foo, bar = 1, c = 2 ]`

		if (!type.classes.containsAll(classes)) return false

		return entities.all { entity ->
			val subEntity = type.entities.resolve(entity.signature)
			if (subEntity != null)
				entity.type.accepts(subEntity.type)
			else
				entity.isInitialized
		}
	}

	override fun toString(): String {
		return "ObjectType<entities = $entities, classes.size = ${classes.size}>"
	}
}

class ClassType(val objectType: ObjectType? = null) : Type() {
//	val simpleInstanceType: ObjectType = ObjectType().apply {
//		inheritFromClass(this@ClassType)
//	}

	override fun accepts(type: Type): Boolean = false

	override fun toString(): String {
		return "ClassType"
	}
}

class NullableType(val nonNullType: Type) : Type() {
	override fun accepts(type: Type): Boolean {
		if (type is NullableType) {
			return nonNullType.accepts(type.nonNullType)
		}

		return nonNullType.accepts(type)
	}

	override fun toString(): String {
		return "NullableType<$nonNullType>"
	}
}

// see the "FunctionType" rule in the grammar
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

class IntType(val minValue: Int = Int.MIN_VALUE, val maxValue: Int = Int.MAX_VALUE) : Type() {
	override fun accepts(type: Type): Boolean {
		if (type !is IntType) return false

		return type.minValue >= minValue && type.maxValue <= maxValue
	}
}
