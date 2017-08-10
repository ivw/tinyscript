package tinyscript.compiler.core

class Operator(
		val name: String,
		val lhsType: FinalType?,
		val rhsType: FinalType,
		val returnType: Type
)

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
