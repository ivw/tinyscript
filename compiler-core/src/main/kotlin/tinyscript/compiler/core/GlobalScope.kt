package tinyscript.compiler.core

val stringClass = ClassType(ObjectType())
val intClass = ClassType(ObjectType())
val floatClass = ClassType(ObjectType())
val booleanClass = ClassType(ObjectType())

val globalScope: Scope = Scope(null, SignatureCollection().apply {
	addSymbol(Symbol("String", stringClass))
	addSymbol(Symbol("Int", intClass))
	addSymbol(Symbol("Float", floatClass))
	addSymbol(Symbol("Boolean", booleanClass))
	addSymbol(Symbol("println", FunctionType(
			ObjectType(SignatureCollection().apply {
				addSymbol(Symbol("m", stringClass.objectType))
			}),
			AnyType
	)))

	addOperator(Operator(
			"+",
			intClass.objectType,
			intClass.objectType,
			intClass.objectType
	))
	addOperator(Operator(
			"*",
			intClass.objectType,
			intClass.objectType,
			intClass.objectType
	))
})
