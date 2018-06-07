package tinyscript.compiler.scope

class AmbiguousSignatureException : RuntimeException(
	"ambiguous signatures"
)

class SignatureMap<K, V> {
	private val entries: MutableList<Entry<K, V>> = arrayListOf()

	fun get(predicate: (K) -> Boolean): Entry<K, V>? {
		val hits = entries.filter { predicate(it.signature) }
		if (hits.size > 1)
			throw AmbiguousSignatureException()
		return if (hits.size == 1) hits[0] else null
	}

	fun add(signature: K, value: V) {
		entries.add(Entry(signature, value, entries.size))
	}

	class Entry<out K, out V>(val signature: K, val value: V, val index: Int)
}
