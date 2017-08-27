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

class Operator(
		val name: String,
		val lhsType: FinalType?,
		val rhsType: FinalType,
		type: Type,
		isAbstract: Boolean = false
) : Signature(type, isAbstract) {
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
	override fun toString(): String {
		return "$name: $type"
	}
}

class SignatureCollection {
	val symbols: MutableMap<String, Symbol> = LinkedHashMap()
	val operators: MutableList<Operator> = ArrayList()
	val methods: MutableList<Method> = ArrayList()

	fun inheritSignatures(objectType: ObjectType) {
		TODO()
	}

	fun getSymbol(name: String): Symbol? {
		return symbols[name]
	}

	fun addSymbol(symbol: Symbol) {
		symbols[symbol.name]?.let { superSymbol ->
			if (!superSymbol.type.final().accepts(symbol.type.final()))
				throw RuntimeException("incompatible override on field '${symbol.name}': ${superSymbol.type} does not accept ${symbol.type}")
		}

		symbols[symbol.name] = symbol
	}

	fun getOperator(name: String, lhsType: FinalType?, rhsType: FinalType): Operator? {
		return if (lhsType == null) {
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
	}

	fun addOperator(operator: Operator) {
		operators.add(operator)
	}

	fun getMethod(name: String, arguments: ObjectType): Method? {
		return methods.findLast { method ->
			method.name == name && method.params.accepts(arguments)
		}
	}

	fun addMethod(method: Method) {
		methods.add(method)
	}
}
