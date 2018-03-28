package tinyscript.compiler.util

interface Lazy<out T> {
	fun get(): T
}

// `isRoot` should be true only if not called from inside a `finalize` function
class SafeLazy<out T>(private val finalize: (isRoot: Boolean) -> T): Lazy<T> {
	private var value: T? = null

	val isFinalized: Boolean get() = value != null

	var isFinalizing: Boolean = false
		private set

	override fun get(): T = get(false)

	private fun get(isRoot: Boolean): T =
		value ?: run {
			if (isFinalizing) throw CycleException()

			isFinalizing = true
			return finalize(isRoot).also {
				value = it
				isFinalizing = false
			}
		}

	private class CycleException : RuntimeException()
}

//class MappedLazy<T, out R>(
//	val lazy: Lazy<T>,
//	val transform: (T) -> R
//): Lazy<R> {
//	override fun get(): R = transform(lazy.get())
//}
//
//fun <T,R> Lazy<T>.map(transform: (T) -> R) =
//	MappedLazy(this, transform)