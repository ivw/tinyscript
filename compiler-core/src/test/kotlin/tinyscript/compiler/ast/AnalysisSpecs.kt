package tinyscript.compiler.ast

import org.antlr.v4.runtime.CharStreams
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import tinyscript.compiler.parser.parseFile
import tinyscript.compiler.stdlib.StandardLibrary
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val specDelimiter = Pattern.compile("////")
private const val errorArgumentPrefix = "// error = "

class AnalysisSpec(
	val title: String,
	val expectedError: String?,
	val content: String
)

fun AnalysisSpec.analyse(): Exception? {
	return try {
		val fileCtx = parseFile(CharStreams.fromString(content))
		fileCtx.declaration().analyse(StandardLibrary.scope)
		null
	} catch (e: Exception) {
		e
	}
}

fun readSpec(spec: String): AnalysisSpec {
	val lines = spec.lines()

	val title = lines[0].trim()

	val lineUnderDelimiter = lines[1]
	val expectedError = if (lineUnderDelimiter.startsWith(errorArgumentPrefix)) {
		lineUnderDelimiter.substring(errorArgumentPrefix.length)
	} else null

	val content = lines.subList(
		if (expectedError != null) 2 else 1,
		lines.size
	).joinToString("\n")

	return AnalysisSpec(title, expectedError, content)
}

fun readFile(file: Path): List<AnalysisSpec> {
	val list: MutableList<AnalysisSpec> = ArrayList()
	Scanner(file).use { scanner ->
		scanner.useDelimiter(specDelimiter)
		while (scanner.hasNext()) {
			list.add(readSpec(scanner.next()))
		}
	}
	return list
}

object AnalysisSpecs : Spek({
	Files.walk(Paths.get("src/test/analysis_specs"))
		.filter { Files.isRegularFile(it) }
		.forEach { file ->
			describe(file.fileName.toString()) {
				readFile(file).forEachIndexed { index, spec ->
					describe("$index ${spec.title}") {
						val analysisError = spec.analyse()
						if (spec.expectedError != null) {
							it("returns an \"${spec.expectedError}\" error") {
								assertEquals(analysisError?.let { it.javaClass.simpleName }, spec.expectedError)
							}
						} else {
							it("returns no error") {
								assertNull(analysisError)
							}
						}
					}
				}
			}
		}
})
