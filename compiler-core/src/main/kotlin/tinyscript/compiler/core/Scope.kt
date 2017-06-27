package tinyscript.compiler.core

open class Scope(val parentScope: Scope?) {
	val symbols: MutableMap<String, Symbol> = LinkedHashMap()

	fun defineSymbol(symbol: Symbol) {
		if (symbols.containsKey(symbol.name))
			throw RuntimeException("Name '${symbol.name}' already exists in this scope")

		symbols[symbol.name] = symbol
	}

	open fun resolveSymbol(name: String): Symbol? {
		return symbols[name] ?: parentScope?.resolveSymbol(name)
	}

	fun resolveSymbolOrFail(name: String): Symbol {
		return resolveSymbol(name) ?: throw RuntimeException("unresolved symbol '$name'")
	}
}

class GlobalScope : Scope(null) {
	override fun resolveSymbol(name: String): Symbol? {
		return super.resolveSymbol(name) ?: builtInSymbols[name]
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
		return super.resolveSymbol(name) ?: objectType.symbols[name]
	}
}

class FunctionScope(parentScope: Scope?, val params: ObjectType) : Scope(parentScope) {
	override fun resolveSymbol(name: String): Symbol? {
		return super.resolveSymbol(name) ?: params.symbols[name]
	}
}
