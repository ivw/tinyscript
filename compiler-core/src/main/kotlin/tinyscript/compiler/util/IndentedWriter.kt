package tinyscript.compiler.util

import java.io.Writer

class IndentedWriter(
	val out: Writer,
	var indent: Int = 0,
	val indentationString: String = "  ",
	val newlineString: String = "\n"
) {
	private var shouldWriteIndentation: Boolean = true

	fun write(str: String) {
		if (shouldWriteIndentation) {
			repeat(indent, {
				out.write(indentationString)
			})
			shouldWriteIndentation = false
		}
		out.write(str)
	}

	fun newLine() {
		out.write(newlineString)
		shouldWriteIndentation = true
	}
}
