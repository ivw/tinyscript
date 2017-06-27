package tinyscript.compiler.core

class Symbol(
		val name: String,
		val type: Type,
		val isAbstract: Boolean = false,
		var isPrivate: Boolean = false,
		val isOverride: Boolean = false,
		var isMutable: Boolean = false
) {
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

val objectType = ObjectType(true, null)
val nullType = NullableType(objectType) // maybe it should be `NullableType(Any)`
val objectClass = ClassType(objectType)
val voidType = ObjectType(true, objectType)
val voidClass = ClassType(voidType)
val stringClass = ClassType(ObjectType(true, objectType))
val intClass = ClassType(ObjectType(true, objectType))
val floatClass = ClassType(ObjectType(true, objectType))
val booleanClass = ClassType(ObjectType(true, objectType))

val builtInSymbols: Map<String, Symbol> = SymbolMapBuilder()
		.add(Symbol("Object", objectClass))
		.add(Symbol("Void", voidClass))
		.add(Symbol("String", stringClass))
		.add(Symbol("Int", intClass))
		.add(Symbol("Float", floatClass))
		.add(Symbol("Boolean", booleanClass))
		.add(Symbol("println", FunctionType(
				ObjectType(false, objectType, SymbolMapBuilder()
						.add(Symbol("m", stringClass.objectType))
						.build()),
				voidType
		)))
		.build()
