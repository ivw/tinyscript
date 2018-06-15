package tinyscript.compiler.scope

val intType = IntType(Int.MIN_VALUE, Int.MAX_VALUE)

val floatType = FloatType(Double.MIN_VALUE, Double.MAX_VALUE)

val stringType = AtomicType(false)

val intrinsicsScope: Scope = object : Scope(null) {
	private val typeSignatureMap: TypeSignatureMap<Type> = TypeSignatureMap<Type>().apply {
		add("Int", false, intType)
		add("Float", false, floatType)
		add("String", false, stringType)
	}

	override fun findType(name: String, isMutable: Boolean): TypeResult? =
		typeSignatureMap.get(name, isMutable)?.let { TypeResult(this, it) }

	override fun findOperator(lhsType: Type?, operatorSymbol: String, isImpure: Boolean, rhsType: Type): ValueResult? {
		if (lhsType is IntType && operatorSymbol == "*" && !isImpure && rhsType is IntType) {
			val returnType = IntType(
				lhsType.minValue + rhsType.minValue,
				lhsType.maxValue + rhsType.maxValue
			)
			return IntrinsicsValueResult(this, returnType, "*")
		}

		return null
	}
}
