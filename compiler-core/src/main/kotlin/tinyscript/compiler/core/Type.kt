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


// see the "objectType" rule in the grammar
open class ObjectType(
		val isNominal: Boolean,
		val superObjectType: ObjectType?,
		val symbols: LinkedHashMap<String, Symbol> = LinkedHashMap()
) : FinalType {
	open fun hasIdentity(type: Type): Boolean {
		return this === type || (superObjectType != null && superObjectType.hasIdentity(type))
	}

	open fun checkConcrete() {
		symbols.values.forEach { symbol ->
			if (symbol.isAbstract) throw RuntimeException("field '${symbol.name}' not initialized")
		}
	}

	override fun accepts(type: FinalType): Boolean {
		if (type !is ObjectType) return false

		// TODO superObjectType has to accept type too? (in case super is nominal and this isn't)

		if (isNominal) return type.hasIdentity(this)

		for ((name, symbol) in symbols) {
			val subSymbol: Symbol = type.symbols[name]
					?: throw RuntimeException("missing symbol '$name'")

			if (!symbol.type.final().accepts(subSymbol.type.final()))
				return false
		}
		return true
	}

	override fun toString(): String {
		return "ObjectType<isNominal = $isNominal, symbols = [${symbols.values.joinToString()}]>"
	}
}


class MergeObjectType(
		val supertype: ObjectType,
		val subtype: ObjectType
) : ObjectType(false, null, mergeSymbols(supertype, subtype)) {
	companion object {
		fun mergeSymbols(supertype: ObjectType, subtype: ObjectType): LinkedHashMap<String, Symbol> {
			val symbols: LinkedHashMap<String, Symbol> = LinkedHashMap(supertype.symbols)
			for (subSymbol in subtype.symbols.values) {
				symbols[subSymbol.name]?.let { superSymbol ->
					if (!superSymbol.type.final().accepts(subSymbol.type.final()))
						throw RuntimeException("incompatible override on field '${subSymbol.name}'")
				}

				symbols[subSymbol.name] = subSymbol
			}
			return symbols
		}
	}

	override fun accepts(type: FinalType): Boolean {
		return supertype.accepts(type) && subtype.accepts(type)
	}

	override fun hasIdentity(type: Type): Boolean {
		return supertype.hasIdentity(type) || subtype.hasIdentity(type)
	}
}


class ClassType(val objectType: ObjectType) : ObjectType(true, objectType)


// see the "NullableType" rule in the grammar
class NullableType(val nonNullType: FinalType) : FinalType {
	override fun accepts(type: FinalType): Boolean {
		if (type === nullType) return true

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
