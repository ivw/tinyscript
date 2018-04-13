package tinyscript.compiler.util

// `isRoot` should be true only if not called from inside a `finalize` function
class SafeLazy<out T>(private val finalize: (isRoot: Boolean) -> T) {
	private var value: T? = null

	val isFinalized: Boolean get() = value != null

	var isFinalizing: Boolean = false
		private set

	fun get(isRoot: Boolean = false): T =
		value ?: run {
			if (isFinalizing) throw CycleException()

			isFinalizing = true
			finalize(isRoot).also {
				value = it
				isFinalizing = false
			}
		}

	class CycleException : RuntimeException()
}