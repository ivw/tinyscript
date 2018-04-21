package tinyscript.compiler.scope

import tinyscript.compiler.ast.AnalysisException

class SignatureMap<V> {
	private val entries: MutableList<Entry<V>> = arrayListOf()

	fun get(signature: Signature): Entry<V>? {
		val hits = entries.filter { it.signature.accepts(signature) }
		if (hits.size > 1)
			throw AnalysisException("ambiguous signatures")
		return if (hits.size == 1) hits[0] else null
	}

	fun add(signature: Signature, value: V) {
		entries.add(Entry(signature, value, entries.size))
	}

	class Entry<out V>(val signature: Signature, val value: V, val index: Int)
}