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

val objectType = ObjectType(true)
val objectClass = ClassType(objectType)
val stringClass = ClassType(ObjectType(true, objectType.identities))
val intClass = ClassType(ObjectType(true, objectType.identities))
val floatClass = ClassType(ObjectType(true, objectType.identities))
val booleanClass = ClassType(ObjectType(true, objectType.identities))

val builtInSymbols: Map<String, Symbol> = SymbolMapBuilder()
		.add(Symbol("Object", objectClass))
		.add(Symbol("String", stringClass))
		.add(Symbol("Int", intClass))
		.add(Symbol("Float", floatClass))
		.add(Symbol("Boolean", booleanClass))
		.add(Symbol("println", FunctionType(
				ObjectType(false, emptySet(), SymbolMapBuilder()
						.add(Symbol("m", stringClass.objectType))
						.build()),
				AnyType
		)))
		.build()
