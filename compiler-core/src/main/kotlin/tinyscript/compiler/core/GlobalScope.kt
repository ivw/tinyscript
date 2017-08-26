package tinyscript.compiler.core

val stringClass = ClassType(ObjectType())
val intClass = ClassType(ObjectType())
val floatClass = ClassType(ObjectType())
val booleanClass = ClassType(ObjectType())

val globalScope: Scope = run {
	val scope = LocalScope(null)

	scope.defineSymbol(Symbol("String", stringClass))
	scope.defineSymbol(Symbol("Int", intClass))
	scope.defineSymbol(Symbol("Float", floatClass))
	scope.defineSymbol(Symbol("Boolean", booleanClass))
	scope.defineSymbol(Symbol("println", FunctionType(
			ObjectType(SymbolMapBuilder()
					.add(Symbol("m", stringClass.objectType))
					.build()),
			AnyType
	)))

	scope.defineOperator(Operator(
			"+",
			intClass.objectType,
			intClass.objectType,
			intClass.objectType
	))
	scope.defineOperator(Operator(
			"*",
			intClass.objectType,
			intClass.objectType,
			intClass.objectType
	))
	scope
}
