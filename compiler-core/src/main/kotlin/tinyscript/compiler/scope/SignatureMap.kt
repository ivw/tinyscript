package tinyscript.compiler.scope

class SignatureMap<V> {
	private val entries: MutableList<Entry<V>> = arrayListOf()

	fun get(signature: Signature): Entry<V>? {
		return entries.find { it.signature.accepts(signature) }
	}

	fun add(signature: Signature, value: V) {
		entries.add(Entry(signature, value, entries.size))
	}

	class Entry<out V>(val signature: Signature, val value: V, val index: Int)
}
