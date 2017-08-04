package tinyscript.compiler.core

abstract class Scope(val parentScope: Scope?) {
	abstract fun resolveSymbol(name: String): Symbol?

	fun resolveSymbolOrFail(name: String): Symbol {
		return resolveSymbol(name) ?: throw RuntimeException("unresolved symbol '$name'")
	}

	abstract fun defineSymbol(symbol: Symbol)
}

open class LocalScope(parentScope: Scope?) : Scope(parentScope) {
	val symbols: MutableMap<String, Symbol> = LinkedHashMap()

	override fun resolveSymbol(name: String): Symbol? {
		return symbols[name] ?: parentScope?.resolveSymbol(name)
	}

	override fun defineSymbol(symbol: Symbol) {
		if (symbols.containsKey(symbol.name))
			throw RuntimeException("name '${symbol.name}' already exists in this scope")

		symbols[symbol.name] = symbol
	}
}

class GlobalScope : LocalScope(null) {
	override fun resolveSymbol(name: String): Symbol? {
		return symbols[name] ?: builtInSymbols[name]
	}
}

class ObjectScope(parentScope: Scope?, val objectType: ObjectType) : Scope(parentScope) {
	companion object {
		fun resolveObjectScope(scope: Scope): ObjectScope? {
			if (scope is ObjectScope) return scope

			return scope.parentScope?.let { resolveObjectScope(it) }
		}
	}

	override fun resolveSymbol(name: String): Symbol? {
		return objectType.symbols[name] ?: parentScope?.resolveSymbol(name)
	}

	override fun defineSymbol(symbol: Symbol) {
//		if (objectType.symbols.containsKey(symbol.name))
//			throw RuntimeException("name already exists in this object")
//		TODO find something for this. it's not critical, though

		objectType.symbols[symbol.name]?.let { superSymbol ->
			if (!superSymbol.type.final().accepts(symbol.type.final()))
				throw RuntimeException("incompatible override on field '${symbol.name}': ${superSymbol.type} does not accept ${symbol.type}")
		}

		objectType.symbols[symbol.name] = symbol
	}
}

class FunctionScope(parentScope: Scope?, val params: ObjectType) : LocalScope(parentScope) {
	override fun resolveSymbol(name: String): Symbol? {
		return symbols[name] ?: params.symbols[name] ?: parentScope?.resolveSymbol(name)
	}
}
