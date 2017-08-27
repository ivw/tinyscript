package tinyscript.compiler.core

import org.antlr.v4.runtime.ParserRuleContext
import tinyscript.compiler.core.parser.TinyScriptParser
import java.io.BufferedWriter

class JavascriptGenerator(out: BufferedWriter, val infoMap: Map<ParserRuleContext, AnalysisInfo>) {
	val out = IndentedWriter(out)

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
						out.write(";")
						out.newLine()
					}
					is TinyScriptParser.PrefixOperatorContext -> {
						val operatorInfo = infoMap[signature] as OperatorInfo

						out.write("var \$op")
//						out.write(operatorInfo.operator.identifier) TODO
						out.write(" = (\$0) => (")
						writeExpression(ctx.expression())
						out.write(");")
						out.newLine()
					}
					is TinyScriptParser.InfixOperatorContext -> {
						val operatorInfo = infoMap[signature] as OperatorInfo

						out.write("var \$op")
//						out.write(operatorInfo.operator.identifier) TODO
						out.write(" = (\$0, \$1) => (")
						writeExpression(ctx.expression())
						out.write(");")
						out.newLine()
					}
					is TinyScriptParser.MethodContext -> {
						val methodInfo = infoMap[signature] as MethodInfo

						out.write("var \$method")
//						out.write(methodInfo.method.identifier) TODO
						out.write(" = ")
						writeFunction(signature.`object`(), ctx.expression())
						out.write(";")
						out.newLine()
					}
					else -> throw RuntimeException("unknown signature type")
				}
			}
			is TinyScriptParser.ImplicitDeclarationContext -> {
				writeExpression(ctx.expression())
				out.write(";")
				out.newLine()
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
				if (objectScope != null && objectScope.objectType.signatures.symbols[name] === referenceExpressionInfo.symbol) {
					out.write("this.")
				}
				out.write(name)
			}
			is TinyScriptParser.DotReferenceExpressionContext -> {
				writeExpression(ctx.expression())
				out.write(".")
				out.write(ctx.Name().text)
			}
			is TinyScriptParser.ObjectExpressionContext -> {
				out.write("new (")
				writeClass(ctx.`object`())
				out.write(")()")
			}
			is TinyScriptParser.FunctionCallExpressionContext -> {
				val analysisInfo = infoMap[ctx] as FunctionCallExpressionInfo
				writeFunctionCall(ctx, analysisInfo.functionType)
			}
			is TinyScriptParser.PrefixOperatorCallExpressionContext -> {
				val analysisInfo = infoMap[ctx] as OperatorCallExpressionInfo
				out.write("\$op")
//				out.write(analysisInfo.operator.identifier) TODO
				out.write("(")
				writeExpression(ctx.expression())
				out.write(")")
			}
			is TinyScriptParser.InfixOperatorCallExpressionContext -> {
				val analysisInfo = infoMap[ctx] as OperatorCallExpressionInfo
				out.write("\$op")
//				out.write(analysisInfo.operator.identifier) TODO
				out.write("(")
				writeExpression(ctx.expression(0))
				out.write(", ")
				writeExpression(ctx.expression(1))
				out.write(")")
			}
			is TinyScriptParser.ClassExpressionContext -> writeClass(ctx.`object`())
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
			is TinyScriptParser.FunctionExpressionContext -> writeFunction(ctx.`object`(), ctx.expression())
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

		out.write("(")
		out.indent++
		out.newLine()

		val nrConditions = ctx.block().size
		for (i in 0 until nrConditions) {
			writeBlock(ctx.block(i))
			out.write(" ? (")
			writeExpression(ctx.expression(i))
			out.write(") :")
			out.newLine()
		}

		writeExpression(ctx.expression(nrConditions))
		out.newLine()

		out.indent--
		out.write(")")
	}

	fun writeFunctionCall(ctx: TinyScriptParser.FunctionCallExpressionContext, functionType: FunctionType) {
		writeExpression(ctx.expression())
		out.write("(")

		val paramIterator = functionType.params.signatures.symbols.values.iterator()

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

	fun writeFunction(objectCtx: TinyScriptParser.ObjectContext?, expressionCtx: TinyScriptParser.ExpressionContext) {
		out.write("(")
		objectCtx?.let { paramsObjectCtx ->
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
		writeExpression(expressionCtx)
		out.write(")")
	}

	fun writeClass(objectCtx: TinyScriptParser.ObjectContext) {
		out.write("function () {")
		out.indent++
		out.newLine()

		for (declaration in objectCtx.declaration()) {
			when (declaration) {
				is TinyScriptParser.AbstractDeclarationContext -> {
					out.write("// this.")
					out.write((declaration.signature() as TinyScriptParser.SymbolContext).Name().text)
					out.write(";")
					out.newLine()
				}
				is TinyScriptParser.ConcreteDeclarationContext -> {
					out.write("this.")
					out.write((declaration.signature() as TinyScriptParser.SymbolContext).Name().text)
					out.write(" = ")
					writeExpression(declaration.expression())
					out.write(";")
					out.newLine()
				}
				is TinyScriptParser.ImplicitDeclarationContext ->
					throw RuntimeException("implicit declaration not allowed here")
				is TinyScriptParser.InheritDeclarationContext -> {
					val analysisInfo = infoMap[declaration] as InheritDeclarationInfo
					when (analysisInfo.expressionType) {
						is ClassType -> {
							out.write("(")
							writeExpression(declaration.expression())
							out.write(").call(this);")
							out.newLine()
						}
						is ObjectType -> {
							out.write("Object.assign(this, ")
							writeExpression(declaration.expression())
							out.write(");")
							out.newLine()
						}
						else -> throw RuntimeException("unsupported expression type")
					}
				}
				else -> throw RuntimeException("unknown declaration type")
			}
		}
		out.indent--
		out.write("}")
	}

	fun writeBlock(ctx: TinyScriptParser.BlockContext) {
		if (ctx.declaration().isEmpty()) {
			out.write("(")
			writeExpression(ctx.expression())
			out.write(")")
		} else {
			out.write("(function () {")
			out.indent++
			out.newLine()
			ctx.declaration().forEach { writeLocalDeclaration(it) }
			out.write("return ")
			writeExpression(ctx.expression())
			out.write(";")
			out.newLine()
			out.indent--
			out.write("})()")
		}
	}
}
