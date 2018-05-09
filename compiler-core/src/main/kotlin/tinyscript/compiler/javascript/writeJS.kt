package tinyscript.compiler.javascript

import tinyscript.compiler.ast.*
import tinyscript.compiler.util.IndentedWriter

fun StatementList.writeJS(out: IndentedWriter) =
	orderedStatements.forEach { it.writeJS(out) }

fun Statement.writeJS(out: IndentedWriter): Unit = when (this) {
	is ImperativeStatement -> {
		if (name != null) {
			out.write("var ")
			out.write(name)
			out.write(" = ")
		}
		expression.writeJS(out)
		out.write(";")
		out.newLine()
	}
	is TypeAliasDefinition -> {
	}
	else -> TODO()
}

fun Expression.writeJS(out: IndentedWriter): Unit = when (this) {
	is BlockExpression -> {
		out.write("(function () {")
		out.indent++
		out.newLine()
		statementList.orderedStatements.forEach { it.writeJS(out) }
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
	is NameReferenceExpression -> {
		out.write(name)
	}
	else -> TODO()
}
