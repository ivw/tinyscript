package tinyscript.compiler.scope

val intType = IntType()

val floatType = FloatType()

val intTimesIntSignature = OperatorSignature(intType, "*", false, intType)

val intrinsicsScope: Scope = SimpleScope(null)
	.apply {
		typeMap["Int"] = intType
		typeMap["Float"] = floatType

		functionMap.add(intTimesIntSignature, { signature ->
			if (signature is OperatorSignature && signature.lhsType is IntType && signature.rhsType is IntType) {
				IntType(
					signature.lhsType.minValue + signature.rhsType.minValue,
					signature.lhsType.maxValue + signature.rhsType.maxValue
				)
			} else throw IllegalStateException()
		})
	}
