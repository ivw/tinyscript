package tinyscript.compiler.core

val stringClass = ClassType()
val intClass = ClassType()
val floatClass = ClassType()
val booleanClass = ClassType()

val globalScope: Scope = Scope(null, SignatureCollection().apply {
	addSymbol(Symbol("String", stringClass))
	addSymbol(Symbol("Int", intClass))
	addSymbol(Symbol("Float", floatClass))
	addSymbol(Symbol("Boolean", booleanClass))
	addSymbol(Symbol("println", FunctionType(
			ObjectType(SignatureCollection().apply {
				addSymbol(Symbol("m", stringClass.simpleInstanceType))
			}),
			AnyType
	)))

	addOperator(Operator(
			"+",
			intClass.simpleInstanceType,
			intClass.simpleInstanceType,
			intClass.simpleInstanceType
	))
	addOperator(Operator(
			"*",
			intClass.simpleInstanceType,
			intClass.simpleInstanceType,
			intClass.simpleInstanceType
	))
})
