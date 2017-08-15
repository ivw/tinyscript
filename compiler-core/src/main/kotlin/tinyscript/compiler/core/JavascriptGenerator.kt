package tinyscript.compiler.core

import org.antlr.v4.runtime.ParserRuleContext
import tinyscript.compiler.core.parser.TinyScriptParser
import java.io.BufferedWriter

class JavascriptGenerator(val out: BufferedWriter, val infoMap: Map<ParserRuleContext, AnalysisInfo>) {
	fun writeFile(ctx: TinyScriptParser.FileContext) {
		ctx.declaration().forEach { writeLocalDeclaration(it) }
	}

	fun writeLocalDeclaration(ctx: TinyScriptParser.DeclarationContext) {
		when (ctx) {
			is TinyScriptParser.AbstractDeclarationContext -> {
			}
			is TinyScriptParser.ConcreteDeclarationContext -> {
				val signature = ctx.signature()
				when (signature) {
					is TinyScriptParser.SymbolContext -> {
						out.write("var ")
						out.write(signature.Name().text)
						out.write(" = ")
						writeExpression(ctx.expression())
						out.write(";\n")
					}
					is TinyScriptParser.PrefixOperatorContext -> {
						val operatorInfo = infoMap[signature] as OperatorInfo

						out.write("var \$op")
						out.write(operatorInfo.operator.identifier)
						out.write(" = (right) => (")
						writeExpression(ctx.expression())
						out.write(");\n")
					}
					is TinyScriptParser.InfixOperatorContext -> {
						val operatorInfo = infoMap[signature] as OperatorInfo

						out.write("var \$op")
						out.write(operatorInfo.operator.identifier)
						out.write(" = (left, right) => (")
						writeExpression(ctx.expression())
						out.write(");\n")
					}
					else -> throw RuntimeException("unknown signature type")
				}
			}
			is TinyScriptParser.ImplicitDeclarationContext -> {
				writeExpression(ctx.expression())
				out.write(";\n")
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
			is TinyScriptParser.SuperExpressionContext -> out.write("super") // TODO
			is TinyScriptParser.ReferenceExpressionContext -> {
				val referenceExpressionInfo = infoMap[ctx] as ReferenceExpressionInfo
				val name = ctx.Name().text
				val objectScope = ObjectScope.resolveObjectScope(referenceExpressionInfo.scope)
				// if the symbol is resolved from objectScope, then we have to generate "this."
				if (objectScope != null && objectScope.objectType.symbols[name] === referenceExpressionInfo.symbol) {
					out.write("this.")
				}
				out.write(name)
			}
			is TinyScriptParser.DotReferenceExpressionContext -> {
				writeExpression(ctx.expression())
				out.write(".")
				out.write(ctx.Name().text)
			}
			is TinyScriptParser.ObjectExpressionContext -> writeObjectInstance(ctx.`object`(), null)
			is TinyScriptParser.ObjectOrCallExpressionContext -> {
				val expressionType: FinalType = (infoMap[ctx.expression()] as ExpressionInfo).type.final()
				when (expressionType) {
					is ClassType -> writeObjectInstance(ctx.`object`(), ctx.expression())
					is FunctionType -> writeFunctionCall(ctx, expressionType)
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
				val analysisInfo = infoMap[ctx] as OperatorCallExpressionInfo
				out.write("\$op")
				out.write(analysisInfo.operator.identifier)
				out.write("(")
				writeExpression(ctx.expression())
				out.write(")")
			}
			is TinyScriptParser.InfixOperatorCallExpressionContext -> {
				val analysisInfo = infoMap[ctx] as OperatorCallExpressionInfo
				out.write("\$op")
				out.write(analysisInfo.operator.identifier)
				out.write("(")
				writeExpression(ctx.expression(0))
				out.write(", ")
				writeExpression(ctx.expression(1))
				out.write(")")
			}
			is TinyScriptParser.ClassExpressionContext -> writeClass(ctx.`object`(), null)
			is TinyScriptParser.ExtendClassExpressionContext -> writeClass(ctx.`object`(), ctx.expression())
			is TinyScriptParser.ConditionalExpressionContext -> writeConditionalExpression(ctx)
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

	fun writeConditionalExpression(ctx: TinyScriptParser.ConditionalExpressionContext) {
		/* example output:
		var message = (
			(hour < 12) ? ("Good morning") :
			(hour > 18) ? ("Good evening") :
			"Good day"
		);
		 */

		out.write("(\n")

		val nrConditions = ctx.block().size
		for (i in 0 until nrConditions) {
			writeBlock(ctx.block(i))
			out.write(" ? (")
			writeExpression(ctx.expression(i))
			out.write(") :\n")
		}

		writeExpression(ctx.expression(nrConditions))

		out.write("\n)")
	}

	fun writeFunctionCall(ctx: TinyScriptParser.ObjectOrCallExpressionContext, functionType: FunctionType) {
		writeExpression(ctx.expression())
		out.write("(")

		val paramIterator = functionType.params.symbols.values.iterator()

		for (declaration in ctx.`object`().declaration()) {
			if (!paramIterator.hasNext())
				throw RuntimeException("too many arguments")
			var param = paramIterator.next()

			when (declaration) {
				is TinyScriptParser.AbstractDeclarationContext -> {
					throw RuntimeException("abstract declaration not allowed here")
				}
				is TinyScriptParser.ConcreteDeclarationContext -> {
					// iterate all skipped params
					val argName = (declaration.signature() as TinyScriptParser.SymbolContext).Name().text
					while (param.name != argName) {
						if (!paramIterator.hasNext())
							throw RuntimeException("invalid argument '$argName'")
						param = paramIterator.next()
						out.write("undefined,")
					}

					writeExpression(declaration.expression())
				}
				is TinyScriptParser.ImplicitDeclarationContext ->
					writeExpression(declaration.expression())
				else -> throw RuntimeException("unknown declaration type")
			}
			out.write(", ")
		}

		out.write(")")
	}

	fun writeFunction(ctx: TinyScriptParser.FunctionExpressionContext) {
		out.write("(")
		ctx.`object`()?.let { paramsObjectCtx ->
			for (declaration in paramsObjectCtx.declaration()) {
				when (declaration) {
					is TinyScriptParser.AbstractDeclarationContext -> {
						out.write((declaration.signature() as TinyScriptParser.SymbolContext).Name().text)
					}
					is TinyScriptParser.ConcreteDeclarationContext -> {
						out.write((declaration.signature() as TinyScriptParser.SymbolContext).Name().text)
						out.write(" = ")
						writeExpression(declaration.expression())
					}
					is TinyScriptParser.ImplicitDeclarationContext ->
						throw RuntimeException("implicit declaration not allowed here")
					else -> throw RuntimeException("unknown declaration type")
				}
				out.write(", ")
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
					out.write((declaration.signature() as TinyScriptParser.SymbolContext).Name().text)
					out.write(";\n")
				}
				is TinyScriptParser.ConcreteDeclarationContext -> {
					out.write("this.")
					out.write((declaration.signature() as TinyScriptParser.SymbolContext).Name().text)
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
			out.write("(function () {\n")
			ctx.declaration().forEach { writeLocalDeclaration(it) }
			out.write("return ")
			writeExpression(ctx.expression())
			out.write(";\n")
			out.write("})()")
		}
	}
}
