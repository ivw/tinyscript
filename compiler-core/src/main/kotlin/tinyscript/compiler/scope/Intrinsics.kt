package tinyscript.compiler.scope

val intType = IntType(Int.MIN_VALUE, Int.MAX_VALUE)

val floatType = FloatType(Double.MIN_VALUE, Double.MAX_VALUE)

val stringType = AtomicType()

val intBoxType = AtomicType()

val intBoxFunctionSignature = NameSignature(
	null,
	"intBox",
	false,
	ObjectType(mapOf("value" to intType))
)

val intBoxSetFunctionSignature = NameSignature(
	intBoxType,
	"set",
	true,
	ObjectType(mapOf("value" to intType))
)

val intBoxGetFunctionSignature = NameSignature(
	intBoxType,
	"get",
	true,
	null
)

val intTimesIntSignature = OperatorSignature(intType, "*", false, intType)

val intrinsicsScope: Scope = SimpleScope(null)
	.apply {
		typeMap["Int"] = intType
		typeMap["Float"] = floatType
		typeMap["String"] = stringType
		typeMap["IntBox"] = intBoxType

		functionMap.add(intTimesIntSignature, { signature ->
			if (signature is OperatorSignature && signature.lhsType is IntType && signature.rhsType is IntType) {
				IntType(
					signature.lhsType.minValue + signature.rhsType.minValue,
					signature.lhsType.maxValue + signature.rhsType.maxValue
				)
			} else throw IllegalStateException()
		})

		functionMap.add(intBoxFunctionSignature, { intBoxType })
		functionMap.add(intBoxSetFunctionSignature, { AnyType })
		functionMap.add(intBoxGetFunctionSignature, { intType })
	}
