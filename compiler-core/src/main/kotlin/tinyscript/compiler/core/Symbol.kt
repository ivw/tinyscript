package tinyscript.compiler.core

class Symbol(
		val name: String,
		val type: Type,
		val isAbstract: Boolean = false,
		var isPrivate: Boolean = false,
		val isOverride: Boolean = false,
		var isMutable: Boolean = false
) {
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
