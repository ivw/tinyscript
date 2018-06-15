package tinyscript.compiler.scope

class TypeSignatureMap<V> {
	private val mutableTypeSignatureMap: MutableMap<String, V> = HashMap()
	private val immutableTypeSignatureMap: MutableMap<String, V> = HashMap()

	fun add(name: String, isMutable: Boolean, value: V) {
		if (isMutable) mutableTypeSignatureMap[name] = value
		else immutableTypeSignatureMap[name] = value
	}

	fun get(name: String, isMutable: Boolean): V? =
		if (isMutable) mutableTypeSignatureMap[name]
		else immutableTypeSignatureMap[name]
}
