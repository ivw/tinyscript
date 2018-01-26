package tinyscript.compiler.util

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

object DeferredSpec : Spek({
	describe("Deferred") {
		it("can") {
			var i = 0
			val deferred = object : Deferred<Int>() {
				override fun finalize(): Int {
					assertTrue(isFinalizing)
					return i
				}
			}

			assertFalse(deferred.isFinalized)
			assertFalse(deferred.isFinalizing)

			i++
			assertEquals(deferred.get(), 1)

			assertTrue(deferred.isFinalized)
			assertFalse(deferred.isFinalizing)

			// it should return the first result
			i++
			assertEquals(deferred.get(), 1)
		}

		it("should throw an error if there is a cycle") {
			val deferred = object : Deferred<String>() {
				override fun finalize(): String {
					return this.get()
				}
			}
			assertFailsWith(RuntimeException::class) {
				deferred.get()
			}
		}
	}
})
