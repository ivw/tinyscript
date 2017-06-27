package tinyscript.compiler.core

import org.antlr.v4.runtime.ParserRuleContext
import tinyscript.compiler.core.parser.TinyScriptParser
import java.io.BufferedWriter

class JavascriptGenerator(val out: BufferedWriter, val typeMap: MutableMap<ParserRuleContext, Type>) {
	fun writeFile(ctx: TinyScriptParser.FileContext) {
		ctx.declaration().forEach { writeLocalDeclaration(it) }
	}

	fun writeLocalDeclaration(ctx: TinyScriptParser.DeclarationContext) {
		when (ctx) {
			is TinyScriptParser.AbstractDeclarationContext -> {
				out.write("// var ")
				out.write(ctx.symbol().Name().text)
				out.write(";\n")
			}
			is TinyScriptParser.ConcreteDeclarationContext -> {
				out.write("var ")
				out.write(ctx.symbol().Name().text)
				out.write(" = ")
				writeExpression(ctx.expression())
				out.write(";\n")
			}
			is TinyScriptParser.ImplicitDeclarationContext -> {
				writeExpression(ctx.expression())
			}
			else -> throw RuntimeException("unknown declaration type")
		}
	}

	fun writeExpression(ctx: TinyScriptParser.ExpressionContext) {
		when (ctx) {
			is TinyScriptParser.BlockExpressionContext -> writeBlock(ctx.block())
			is TinyScriptParser.IntegerLiteralExpressionContext -> out.write(ctx.text)
			is TinyScriptParser.FloatLiteralExpressionContext -> out.write(ctx.text)
			is TinyScriptParser.StringLiteralExpressionContext -> out.write(ctx.text)
			is TinyScriptParser.BooleanLiteralExpressionContext -> out.write(ctx.text)
			is TinyScriptParser.NullExpressionContext -> out.write("null")
			is TinyScriptParser.ThisExpressionContext -> out.write("this")
			is TinyScriptParser.SuperExpressionContext -> out.write("super")
			is TinyScriptParser.ReferenceExpressionContext -> out.write(ctx.Name().text)
			is TinyScriptParser.DotReferenceExpressionContext -> {
				writeExpression(ctx.expression())
				out.write(".")
				out.write(ctx.Name().text)
			}
			is TinyScriptParser.ObjectExpressionContext -> writeObjectInstance(ctx.`object`(), null)
			is TinyScriptParser.ObjectOrCallExpressionContext -> {
				val expressionType: FinalType = typeMap[ctx.expression()]!!.final()
				when (expressionType) {
					is ClassType -> {
						writeObjectInstance(ctx.`object`(), ctx.expression())
					}
					is FunctionType -> {
						writeExpression(ctx.expression())
						out.write("(")
						// TODO
						out.write(")")
					}
					else -> throw RuntimeException("unsupported expression type")
				}
			}
			is TinyScriptParser.ClassMergeExpressionContext -> {
				out.write("function () {\n")

				for (expressionCtx in ctx.expression()) {
					out.write("(")
					writeExpression(expressionCtx)
					out.write(").call(this);\n")
				}

				out.write("}")
			}
			is TinyScriptParser.PrefixOperatorCallExpressionContext -> {
				writeOperator(ctx.Operator().text)
				writeExpression(ctx.expression())
			}
			is TinyScriptParser.InfixOperatorCallExpressionContext -> {
				writeExpression(ctx.expression(0))
				writeOperator(ctx.Operator().text)
				writeExpression(ctx.expression(1))
			}
			is TinyScriptParser.ClassExpressionContext -> writeClass(ctx.`object`(), null)
			is TinyScriptParser.ExtendClassExpressionContext -> writeClass(ctx.`object`(), ctx.expression())
			is TinyScriptParser.ConditionalExpressionContext -> TODO()
			is TinyScriptParser.ReassignmentExpressionContext -> {
				out.write(ctx.Name().text)
				out.write(" = ")
				writeExpression(ctx.expression())
			}
			is TinyScriptParser.DotReassignmentExpressionContext -> {
				writeExpression(ctx.expression(0))
				out.write(".")
				out.write(ctx.Name().text)
				out.write(" = ")
				writeExpression(ctx.expression(1))
			}
			is TinyScriptParser.FunctionExpressionContext -> writeFunction(ctx)
			else -> throw RuntimeException("unknown expression type")
		}
	}

	fun writeOperator(operator: String) {
		out.write(" ")
		out.write(when (operator) {
			"==" -> "==="
			"!=" -> "!=="
			else -> operator
		})
		out.write(" ")
	}

	fun writeFunction(ctx: TinyScriptParser.FunctionExpressionContext) {
		out.write("(")
		ctx.`object`()?.let { paramsObjectCtx ->
			for (declaration in paramsObjectCtx.declaration()) {
				when (declaration) {
					is TinyScriptParser.AbstractDeclarationContext -> {
						out.write(declaration.symbol().Name().text)
					}
					is TinyScriptParser.ConcreteDeclarationContext -> {
						out.write(declaration.symbol().Name().text)
						out.write(" = ")
						writeExpression(declaration.expression())
					}
					is TinyScriptParser.ImplicitDeclarationContext ->
						throw RuntimeException("implicit declaration not allowed here")
					else -> throw RuntimeException("unknown declaration type")
				}
				out.write(",")
			}
		}
		out.write(") => (")
		writeExpression(ctx.expression())
		out.write(")")
	}

	fun writeObjectInstance(ctx: TinyScriptParser.ObjectContext, superExpressionCtx: TinyScriptParser.ExpressionContext?) {
		out.write("new (")
		writeClass(ctx, superExpressionCtx)
		out.write(")()")
	}

	fun writeClass(objectCtx: TinyScriptParser.ObjectContext, superExpressionCtx: TinyScriptParser.ExpressionContext?) {
		out.write("function () {\n")

		if (superExpressionCtx != null) {
			out.write("(")
			writeExpression(superExpressionCtx)
			out.write(").call(this);\n")
		}

		for (declaration in objectCtx.declaration()) {
			when (declaration) {
				is TinyScriptParser.AbstractDeclarationContext -> {
					out.write("// this.")
					out.write(declaration.symbol().Name().text)
					out.write(";\n")
				}
				is TinyScriptParser.ConcreteDeclarationContext -> {
					out.write("this.")
					out.write(declaration.symbol().Name().text)
					out.write(" = ")
					writeExpression(declaration.expression())
					out.write(";\n")
				}
				is TinyScriptParser.ImplicitDeclarationContext ->
					throw RuntimeException("implicit declaration not allowed here")
				else -> throw RuntimeException("unknown declaration type")
			}
		}
		out.write("}")
	}

	fun writeBlock(ctx: TinyScriptParser.BlockContext) {
		if (ctx.declaration().isEmpty()) {
			out.write("(")
			writeExpression(ctx.expression())
			out.write(")")
		} else {
			out.write("(function () {")
			ctx.declaration().forEach { writeLocalDeclaration(it) }
			out.write("return ")
			writeExpression(ctx.expression())
			out.write(";\n")
			out.write("})()")
		}
	}
}
