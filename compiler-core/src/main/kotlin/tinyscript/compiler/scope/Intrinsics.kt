package tinyscript.compiler.scope

val intType = IntType(Int.MIN_VALUE, Int.MAX_VALUE)

val floatType = FloatType(Double.MIN_VALUE, Double.MAX_VALUE)

val stringType = AtomicType(false)

val intrinsicsScope: Scope = object : Scope(null) {
	private val typeMap = mapOf(
		"Int" to intType,
		"Float" to floatType,
		"String" to stringType
	)

	override fun findType(name: String): TypeResult? = typeMap[name]?.let { TypeResult(this, it) }

	override fun findOperator(lhsType: Type?, operatorSymbol: String, isImpure: Boolean, rhsType: Type): ValueResult? {
		if (lhsType is IntType && operatorSymbol == "*" && isImpure == false && rhsType is IntType) {
			val returnType = IntType(
				lhsType.minValue + rhsType.minValue,
				lhsType.maxValue + rhsType.maxValue
			)
			return IntrinsicsValueResult(this, returnType, "*")
		}

		return null
	}
}
