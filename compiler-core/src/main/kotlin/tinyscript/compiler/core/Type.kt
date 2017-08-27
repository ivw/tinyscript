package tinyscript.compiler.core

// see the "type" rule in the grammar
interface Type {
	fun final(): FinalType
}

interface FinalType : Type {
	override fun final(): FinalType = this

	/**
	 * @return true if `type` is equal to this, or if it is a subtype of this
	 */
	fun accepts(type: FinalType): Boolean
}


abstract class DeferredType : Type {
	var finalType: FinalType? = null

	final override fun final(): FinalType {
		finalType?.let { return it }

		val finalType = createFinalType()
		this.finalType = finalType
		return finalType
	}

	protected abstract fun createFinalType(): FinalType

	override fun toString(): String {
		return "DeferredType<$finalType>"
	}
}


// AnyType accepts anything; objects, functions, etc.
// Nothing can be done with values of Any type, not even casting.
object AnyType : FinalType {
	override fun accepts(type: FinalType): Boolean = true

	override fun toString(): String {
		return "AnyType"
	}
}


// see the "objectType" rule in the grammar
open class ObjectType(
		val signatures: SignatureCollection = SignatureCollection(),
		val identities: MutableSet<ObjectType> = HashSet()
) : FinalType {
	override fun accepts(type: FinalType): Boolean {
		if (type !is ObjectType) return false

		if (!type.identities.containsAll(identities)) return false

		for ((name, symbol) in signatures.symbols) {
			val subSymbol = type.signatures.symbols[name]
			if (subSymbol == null) {
				if (symbol.isAbstract)
					return false
			} else {
				if (!symbol.type.final().accepts(subSymbol.type.final()))
					return false
			}
		}
		return true
	}

	override fun toString(): String {
		return "ObjectType<signatures = $signatures, identities.size = ${identities.size}>"
	}
}

fun unionObjectType(a: ObjectType, b: ObjectType): ObjectType {
	val objectType = ObjectType()
	// TODO inherit from a
	// TODO inherit from b
	return objectType
}

fun intersectObjectType(a: ObjectType, b: ObjectType): ObjectType {
	TODO()
}


class ClassType(val objectType: ObjectType) : FinalType {
	override fun accepts(type: FinalType): Boolean = false

	override fun toString(): String {
		return "ClassType<$objectType>"
	}
}


// see the "NullableType" rule in the grammar
class NullableType(val nonNullType: FinalType) : FinalType {
	override fun accepts(type: FinalType): Boolean {
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
class FunctionType(val params: ObjectType, val returnType: Type) : FinalType {
	override fun accepts(type: FinalType): Boolean {
		if (type !is FunctionType) return false

		// [d: Dog] -> Dog   accepts  [d: Animal] -> SpecialDog
		return type.params.accepts(params) && returnType.final().accepts(type.returnType.final())
	}

	override fun toString(): String {
		return "FunctionType<$params -> $returnType>"
	}
}

fun intersectTypes(a: FinalType, b: FinalType): FinalType {
	if (a === b) return a

	val nonNullA: FinalType = if (a is NullableType) a.nonNullType else a
	val nonNullB: FinalType = if (b is NullableType) b.nonNullType else b

	val nonNullIntersectType: FinalType = if (nonNullA is ObjectType && nonNullB is ObjectType) {
		intersectObjectType(nonNullA, nonNullB)
	} else if (nonNullA is FunctionType && nonNullB is FunctionType) {
		FunctionType(
				unionObjectType(nonNullA.params, nonNullB.params),
				intersectTypes(nonNullA.returnType.final(), nonNullB.returnType.final())
		)
	} else {
		AnyType
	}

	return if (a is NullableType || b is NullableType) NullableType(nonNullIntersectType) else nonNullIntersectType
}
