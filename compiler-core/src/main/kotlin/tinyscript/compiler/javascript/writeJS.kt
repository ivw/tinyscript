package tinyscript.compiler.javascript

import tinyscript.compiler.core.*
import tinyscript.compiler.util.IndentedWriter

fun DeclarationCollection.writeJS(out: IndentedWriter): Unit = TODO()

fun Declaration.writeJS(out: IndentedWriter): Unit = TODO()

fun Expression.writeJS(out: IndentedWriter): Unit = when (this) {
	is BlockExpression -> {
		if (declarationCollection.orderedDeclarations.isEmpty()) {
			out.write("(")
			expression.writeJS(out)
			out.write(")")
		} else {
			out.write("(function () {")
			out.indent++
			out.newLine()
			declarationCollection.orderedDeclarations.forEach { it.writeJS(out) }
			out.write("return ")
			expression.writeJS(out)
			out.write(";")
			out.newLine()
			out.indent--
			out.write("})()")
		}
	}
	is IntExpression -> {
		out.write(value.toString())
	}
	else -> TODO()
}
