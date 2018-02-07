package tinyscript.compiler.javascript

import tinyscript.compiler.core.*
import tinyscript.compiler.util.IndentedWriter

fun DeclarationCollection.writeJS(out: IndentedWriter) =
	orderedDeclarations.forEach { it.writeJS(out) }

fun Declaration.writeJS(out: IndentedWriter): Unit = when (this) {
	is NameDeclaration -> {
		out.write("var ")
		out.write(name)
		out.write(" = ")
		initializer.expression.writeJS(out)
		out.write(";")
		out.newLine()
	}
	is FunctionDeclaration -> {
		out.write("var ")
		out.write(name)
		out.write(" = (TODO) => ")
		initializer.expression.writeJS(out)
		out.write(";")
		out.newLine()
	}
	is TypeAliasDeclaration -> {
	}
	is NonDeclaration ->
		expression.writeJS(out)
}

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
	is ObjectExpression -> {
		// TODO
	}
	is ReferenceExpression -> {
		out.write(name)
	}
}
