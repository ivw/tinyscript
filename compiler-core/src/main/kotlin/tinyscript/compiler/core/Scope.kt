package tinyscript.compiler.core

abstract class Scope(val parentScope: Scope?) {
	abstract val symbols: MutableMap<String, Symbol>

	abstract val operators: MutableList<Operator>

	abstract val methods: MutableList<Method>

	val depth: Int = if (parentScope == null) 0 else parentScope.depth + 1

	open fun resolveSymbol(name: String): Symbol? {
		return symbols[name] ?: parentScope?.resolveSymbol(name)
	}

	fun resolveSymbolOrFail(name: String): Symbol {
		return resolveSymbol(name) ?: throw RuntimeException("unresolved symbol '$name'")
	}

	open fun defineSymbol(symbol: Symbol) {
		symbols[symbol.name] = symbol
	}

	open fun resolveOperator(name: String, lhsType: FinalType?, rhsType: FinalType): Operator? {
		val operator = if (lhsType == null) {
			operators.findLast { operator ->
				operator.name == name
						&& operator.lhsType == null
						&& operator.rhsType.accepts(rhsType)
			}
		} else {
			operators.findLast { operator ->
				operator.name == name
						&& operator.lhsType != null && operator.lhsType.accepts(lhsType)
						&& operator.rhsType.accepts(rhsType)
			}
		}
		return operator ?: parentScope?.resolveOperator(name, lhsType, rhsType)
	}

	open fun defineOperator(operator: Operator) {
		operator.identifier = "${depth}_${operators.size}"
		operators.add(operator)
	}

	open fun resolveMethod(name: String, arguments: ObjectType): Method? {
		val method = methods.findLast { method ->
			method.name == name && method.params.accepts(arguments)
		}
		return method ?: parentScope?.resolveMethod(name, arguments)
	}

	open fun defineMethod(method: Method) {
		method.identifier = "${depth}_${methods.size}"
		methods.add(method)
	}
}

open class LocalScope(
		parentScope: Scope?,
		override val symbols: MutableMap<String, Symbol> = LinkedHashMap(),
		override val operators: MutableList<Operator> = ArrayList(),
		override val methods: MutableList<Method> = ArrayList()
) : Scope(parentScope) {
	override fun defineSymbol(symbol: Symbol) {
		if (symbols.containsKey(symbol.name))
			throw RuntimeException("name '${symbol.name}' already exists in this scope")

		super.defineSymbol(symbol)
	}
}

class ObjectScope(parentScope: Scope?, val objectType: ObjectType) : Scope(parentScope) {
	companion object {
		fun resolveObjectScope(scope: Scope): ObjectScope? {
			if (scope is ObjectScope) return scope

			return scope.parentScope?.let { resolveObjectScope(it) }
		}
	}

	override val symbols: MutableMap<String, Symbol> get() = objectType.symbols

	override val operators: MutableList<Operator> get() = ArrayList() // TODO

	override val methods: MutableList<Method> get() = ArrayList() // TODO

	override fun defineSymbol(symbol: Symbol) {
//		if (objectType.symbols.containsKey(symbol.name))
//			throw RuntimeException("name already exists in this object")
//		TODO find something for this. it's not critical, though

		objectType.symbols[symbol.name]?.let { superSymbol ->
			if (!superSymbol.type.final().accepts(symbol.type.final()))
				throw RuntimeException("incompatible override on field '${symbol.name}': ${superSymbol.type} does not accept ${symbol.type}")
		}

		super.defineSymbol(symbol)
	}
}

class FunctionScope(parentScope: Scope?, val params: ObjectType) : LocalScope(parentScope) {
	override fun resolveSymbol(name: String): Symbol? {
		return symbols[name] ?: params.symbols[name] ?: parentScope?.resolveSymbol(name)
	}
}
