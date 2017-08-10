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
	override fun toString(): String {
		return "$name: $type"
	}
}

class OperatorList {
	private val operators: MutableList<Operator> = ArrayList()

	fun add(operator: Operator) {
		// here, checks could be done to disallow overlapping, for example.

		operators.add(operator)
	}

	fun resolve(name: String, lhsType: FinalType?, rhsType: FinalType): Operator? {
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
}
