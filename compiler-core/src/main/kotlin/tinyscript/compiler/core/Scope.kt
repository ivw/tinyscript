package tinyscript.compiler.core

abstract class Scope(val parentScope: Scope?) {
	abstract fun resolveSymbol(name: String): Symbol?

	fun resolveSymbolOrFail(name: String): Symbol {
		return resolveSymbol(name) ?: throw RuntimeException("unresolved symbol '$name'")
	}
}

open class LocalScope(parentScope: Scope?) : Scope(parentScope) {
	val symbols: MutableMap<String, Symbol> = LinkedHashMap()

	fun defineSymbol(symbol: Symbol) {
		if (symbols.containsKey(symbol.name))
			throw RuntimeException("name '${symbol.name}' already exists in this scope")

		symbols[symbol.name] = symbol
	}

	override fun resolveSymbol(name: String): Symbol? {
		return symbols[name] ?: parentScope?.resolveSymbol(name)
	}
}

class GlobalScope : LocalScope(null) {
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
		return objectType.symbols[name] ?: parentScope?.resolveSymbol(name)
	}
}

class FunctionScope(parentScope: Scope?, val params: ObjectType) : LocalScope(parentScope) {
	override fun resolveSymbol(name: String): Symbol? {
		return symbols[name] ?: params.symbols[name] ?: parentScope?.resolveSymbol(name)
	}
}
