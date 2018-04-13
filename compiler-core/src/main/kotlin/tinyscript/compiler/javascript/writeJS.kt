package tinyscript.compiler.javascript

import tinyscript.compiler.core.*
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
	is TypeAliasDeclaration -> {
	}
	else -> TODO()
}

fun Expression.writeJS(out: IndentedWriter): Unit = when (this) {
	is BlockExpression -> {
		if (statementList == null || statementList.orderedStatements.isEmpty()) {
			out.write("(")
			expression.writeJS(out)
			out.write(")")
		} else {
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
	is FunctionCallExpression -> {
		out.write(name)
		out.write("(")
		argumentsObjectExpression.writeJS(out)
		out.write(")")
	}
	is NameReferenceExpression -> {
		out.write(name)
	}
	else -> TODO()
}
