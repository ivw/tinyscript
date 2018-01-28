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
			val deferred = Deferred { i }

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
			var deferred1: Deferred<Int>? = null
			val deferred2: Deferred<Int> = Deferred { deferred1!!.get() }
			deferred1 = Deferred { deferred2.get() }
			assertFailsWith(RuntimeException::class) {
				deferred2.get()
			}
		}
	}
})
