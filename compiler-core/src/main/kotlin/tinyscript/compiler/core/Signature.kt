package tinyscript.compiler.core

open class Signature(
		val type: Type,
		val isAbstract: Boolean = false
)

class Symbol(
		val name: String,
		type: Type,
		isAbstract: Boolean = false,
		val isMutable: Boolean = false
) : Signature(type, isAbstract) {

	override fun toString(): String {
		return "$name: $type"
	}
}

class SymbolMapBuilder {
	private val symbolMap = LinkedHashMap<String, Symbol>()

	fun add(symbol: Symbol): SymbolMapBuilder {
		symbolMap[symbol.name] = symbol
		return this
	}

	fun build(): LinkedHashMap<String, Symbol> {
		return symbolMap
	}
}

class Operator(
		val name: String,
		val lhsType: FinalType?,
		val rhsType: FinalType,
		type: Type,
		isAbstract: Boolean = false
) : Signature(type, isAbstract) {
	lateinit var identifier: String // scope-unique identifier, with the shape "$depth_$index", used for generated code

	override fun toString(): String {
		return "$name: $type"
	}
}

class Method(
		val name: String,
		val params: ObjectType,
		type: Type,
		isAbstract: Boolean = false
) : Signature(type, isAbstract) {
	lateinit var identifier: String // scope-unique identifier, with the shape "$depth_$index", used for generated code

	override fun toString(): String {
		return "$name: $type"
	}
}
