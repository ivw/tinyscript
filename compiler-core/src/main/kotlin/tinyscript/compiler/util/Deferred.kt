package tinyscript.compiler.util

class Deferred<out T>(private val finalize: () -> T) {
	private var value: T? = null

	val isFinalized: Boolean get() = value != null

	var isFinalizing: Boolean = false
		private set

	fun get(): T =
		value ?: run {
			if (isFinalizing) throw RuntimeException("cycle detected")

			isFinalizing = true
			return finalize().also {
				value = it
				isFinalizing = false
			}
		}
}
