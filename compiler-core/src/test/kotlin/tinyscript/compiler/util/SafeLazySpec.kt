package tinyscript.compiler.util

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

object SafeLazySpec : Spek({
	describe("SafeLazy") {
		it("works") {
			var i = 0
			val lazy = SafeLazy { i }

			assertFalse(lazy.isFinalized)
			assertFalse(lazy.isFinalizing)

			i++
			assertEquals(lazy.get(), 1)

			assertTrue(lazy.isFinalized)
			assertFalse(lazy.isFinalizing)

			// it should return the first result
			i++
			assertEquals(lazy.get(), 1)
		}

		it("should throw an error if there is a cycle") {
			var lazy1: SafeLazy<Int>? = null
			val lazy2: SafeLazy<Int> = SafeLazy { lazy1!!.get() }
			lazy1 = SafeLazy { lazy2.get() }
			assertFailsWith(SafeLazy.CycleException::class) {
				lazy2.get()
			}
		}
	}
})
