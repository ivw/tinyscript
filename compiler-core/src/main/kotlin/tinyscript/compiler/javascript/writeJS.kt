package tinyscript.compiler.javascript

import tinyscript.compiler.ast.*
import tinyscript.compiler.util.IndentedWriter

fun DeclarationList.writeJS(out: IndentedWriter) =
	orderedDeclarations.forEach { it.writeJS(out) }

fun Declaration.writeJS(out: IndentedWriter): Unit = when (this) {
	is TypeAliasDefinition -> {
	}
	else -> TODO()
}

fun Expression.writeJS(out: IndentedWriter): Unit = when (this) {
	is BlockExpression -> {
		out.write("(function () {")
		out.indent++
		out.newLine()
//		blockStatementList.forEach { it.writeJS(out) }
		out.write("return ")
		expression.writeJS(out)
		out.write(";")
		out.newLine()
		out.indent--
		out.write("})()")
	}
	is IntExpression -> {
		out.write(value.toString())
	}
	is FloatExpression -> {
		out.write(value.toString())
	}
	is ObjectExpression -> {
		out.write("[]")
		// TODO
	}
	is NameCallExpression -> {
		out.write(name)
	}
	else -> TODO()
}
