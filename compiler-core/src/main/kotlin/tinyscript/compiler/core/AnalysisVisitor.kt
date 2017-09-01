package tinyscript.compiler.core

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.TerminalNode
import tinyscript.compiler.core.parser.TinyScriptParser
import java.util.*

class AnalysisVisitor(val sourceDescription: String) {
	val infoMap: MutableMap<ParserRuleContext, AnalysisInfo> = HashMap()

	val deferredAnalyses: LinkedList<DeferredType> = LinkedList()

	fun finishDeferredAnalyses() {
		while (deferredAnalyses.isNotEmpty()) {
			val deferredAnalysis = deferredAnalyses.first
			deferredAnalysis.final()
			if (deferredAnalyses.contains(deferredAnalysis))
				throw RuntimeException("deferredAnalysis did not remove itself")
		}
	}

	fun visitFile(ctx: TinyScriptParser.FileContext) {
		val scope = Scope(globalScope, SignatureCollection())
		for (declaration in ctx.declaration()) {
			visitLocalDeclaration(declaration, scope)
		}
	}

	fun visitLocalDeclaration(ctx: TinyScriptParser.DeclarationContext, scope: Scope) {
		when (ctx) {
			is TinyScriptParser.AbstractDeclarationContext -> {
				visitSignatureDeclaration(scope, ctx.signature(), ctx.type(), null)
				// TODO check if `native`?
			}
			is TinyScriptParser.ConcreteDeclarationContext ->
				visitSignatureDeclaration(scope, ctx.signature(), ctx.type(), ctx.expression())
			is TinyScriptParser.ImplicitDeclarationContext ->
				visitExpression(ctx.expression(), scope)
			else -> throw RuntimeException("unknown declaration type")
		}
	}

	fun visitSignatureDeclaration(scope: Scope, signatureCtx: TinyScriptParser.SignatureContext, typeCtx: TinyScriptParser.TypeContext?, expressionCtx: TinyScriptParser.ExpressionContext?): Signature {
		return when (signatureCtx) {
			is TinyScriptParser.SymbolContext -> {
				val expressionType = expressionCtx?.let { visitExpression(it, scope) }
				val typeAnnotationType: FinalType? = typeCtx?.let { visitType(it, scope) }
				val type: Type = typeAnnotationType ?: expressionType ?: throw IllegalStateException()

				val symbol = Symbol(
						signatureCtx.Name().text,
						type,
						expressionType == null,
						signatureCtx.getToken(TinyScriptParser.Mut, 0) != null
				)
				scope.signatures.addSymbol(symbol)

				if (typeAnnotationType != null && expressionType != null) {
					val finalExpressionType = expressionType.final()
					if (!typeAnnotationType.accepts(finalExpressionType))
						throw AnalysisError("invalid value for symbol '$symbol': $typeAnnotationType does not accept $finalExpressionType", sourceDescription, signatureCtx.start)
				}
				symbol
			}
			is TinyScriptParser.PrefixOperatorContext -> {
				val rhsType = visitType(signatureCtx.type(), scope)

				val expressionScope = FunctionScope(scope, ObjectType(
						SignatureCollection().apply {
							addSymbol(Symbol("$0", rhsType))
						}
				))
				val expressionType = expressionCtx?.let { visitExpression(it, expressionScope) }
				val typeAnnotationType: FinalType? = typeCtx?.let { visitType(it, scope) }
				val type: Type = typeAnnotationType ?: expressionType ?: throw IllegalStateException()

				val operator = Operator(
						signatureCtx.Operator().text, null, rhsType, type,
						expressionType == null
				)
				scope.signatures.addOperator(operator)

				if (typeAnnotationType != null && expressionType != null) {
					val finalExpressionType = expressionType.final()
					if (!typeAnnotationType.accepts(finalExpressionType))
						throw AnalysisError("invalid value for operator '$operator': $typeAnnotationType does not accept $finalExpressionType", sourceDescription, signatureCtx.start)
				}

				infoMap[signatureCtx] = OperatorInfo(scope, operator)
				operator
			}
			is TinyScriptParser.InfixOperatorContext -> {
				val lhsType = visitType(signatureCtx.type(0), scope)
				val rhsType = visitType(signatureCtx.type(1), scope)

				val expressionScope = FunctionScope(scope, ObjectType(
						SignatureCollection().apply {
							addSymbol(Symbol("$0", lhsType))
							addSymbol(Symbol("$1", rhsType))
						}
				))
				val expressionType = expressionCtx?.let { visitExpression(it, expressionScope) }
				val typeAnnotationType: FinalType? = typeCtx?.let { visitType(it, scope) }
				val type: Type = typeAnnotationType ?: expressionType ?: throw IllegalStateException()

				val operator = Operator(
						signatureCtx.Operator().text, lhsType, rhsType, type,
						expressionType == null
				)
				scope.signatures.addOperator(operator)

				if (typeAnnotationType != null && expressionType != null) {
					val finalExpressionType = expressionType.final()
					if (!typeAnnotationType.accepts(finalExpressionType))
						throw AnalysisError("invalid value for operator '$operator': $typeAnnotationType does not accept $finalExpressionType", sourceDescription, signatureCtx.start)
				}

				infoMap[signatureCtx] = OperatorInfo(scope, operator)
				operator
			}
			is TinyScriptParser.MethodContext -> {
				val params = visitObject(signatureCtx.`object`(), scope, false)

				val expressionScope = FunctionScope(scope, params)
				val expressionType = expressionCtx?.let { visitExpression(it, expressionScope) }
				val typeAnnotationType: FinalType? = typeCtx?.let { visitType(it, scope) }
				val type: Type = typeAnnotationType ?: expressionType ?: throw IllegalStateException()

				val method = Method(
						signatureCtx.Name().text, params, type,
						expressionType == null
				)
				scope.signatures.addMethod(method)

				if (typeAnnotationType != null && expressionType != null) {
					val finalExpressionType = expressionType.final()
					if (!typeAnnotationType.accepts(finalExpressionType))
						throw AnalysisError("invalid value for method '$method': $typeAnnotationType does not accept $finalExpressionType", sourceDescription, signatureCtx.start)
				}

				infoMap[signatureCtx] = MethodInfo(scope, method)
				method
			}
			else -> throw RuntimeException("unknown Signature type")
		}
	}

	fun visitType(ctx: TinyScriptParser.TypeContext, scope: Scope): FinalType {
		return when (ctx) {
			is TinyScriptParser.ParenTypeContext -> visitType(ctx.type(), scope)
			is TinyScriptParser.FunctionTypeContext -> {
				val paramsObjectType: TinyScriptParser.ObjectTypeContext? = ctx.objectType()
				val params =
						if (paramsObjectType != null) visitObjectType(paramsObjectType, scope)
						else ObjectType()
				FunctionType(params, visitType(ctx.type(), scope))
			}
			is TinyScriptParser.NullTypeContext -> NullableType(AnyType)
			is TinyScriptParser.NullableTypeContext -> NullableType(visitType(ctx.type(), scope))
			is TinyScriptParser.ObjectTypeTypeContext -> visitObjectType(ctx.objectType(), scope)
			is TinyScriptParser.TypeReferenceContext -> {
				val referenceSymbol = scope.resolveSymbol(ctx.Name().text)
						?: throw AnalysisError("unresolved symbol '${ctx.Name().text}'", sourceDescription, ctx.start)
				val classType = referenceSymbol.type.final() as ClassType
				classType.simpleInstanceType
			}
			is TinyScriptParser.IntersectObjectTypeContext -> {
				val leftType = visitType(ctx.type(0), scope) as ObjectType
				val rightType = visitType(ctx.type(1), scope) as ObjectType
				intersectObjectType(leftType, rightType)
			}
			else -> throw RuntimeException("unknown Type type")
		}
	}

	fun visitObjectType(ctx: TinyScriptParser.ObjectTypeContext, scope: Scope): ObjectType {
		val objectType = ObjectType()
		val objectScope = ObjectScope(scope, objectType)
		ctx.objectTypeField().forEach({ objectTypeFieldCtx ->
			when (objectTypeFieldCtx) {
				is TinyScriptParser.SymbolObjectTypeFieldContext -> {
					val type: Type = visitType(objectTypeFieldCtx.type(), objectScope)

					val symbol = Symbol(
							objectTypeFieldCtx.Name().text,
							type,
							true,
							objectTypeFieldCtx.getToken(TinyScriptParser.Mut, 0) != null
					)
					objectScope.signatures.addSymbol(symbol)
				}
				is TinyScriptParser.InheritDeclarationObjectTypeFieldContext -> {
					val referenceSymbol = objectScope.resolveSymbol(objectTypeFieldCtx.Name().text)
							?: throw AnalysisError("unresolved symbol '${objectTypeFieldCtx.Name().text}'", sourceDescription, objectTypeFieldCtx.start)
					val classType = referenceSymbol.type.final() as ClassType
					objectType.inheritFromClass(classType)
				}
				else -> throw RuntimeException("unknown ObjectTypeField type")
			}
		})
		return objectType
	}

	fun visitObject(ctx: TinyScriptParser.ObjectContext, scope: Scope, mustBeConcrete: Boolean): ObjectType {
		val objectType = ObjectType()
		val objectScope = ObjectScope(scope, objectType)

		for (declaration in ctx.declaration()) {
			when (declaration) {
				is TinyScriptParser.AbstractDeclarationContext -> {
					visitSignatureDeclaration(objectScope, declaration.signature(), declaration.type(), null)
				}
				is TinyScriptParser.ConcreteDeclarationContext -> {
					visitSignatureDeclaration(objectScope, declaration.signature(), declaration.type(), declaration.expression())
				}
				is TinyScriptParser.ImplicitDeclarationContext -> TODO()
				is TinyScriptParser.InheritDeclarationContext -> {
					val expressionType = visitExpression(declaration.expression(), objectScope).final()

					when (expressionType) {
						is ClassType -> objectType.inheritFromClass(expressionType)
						is ObjectType -> objectType.inheritFromObject(expressionType)
						else -> throw AnalysisError("unsupported expression type '$expressionType'", sourceDescription, declaration.start)
					}
					infoMap[declaration] = InheritDeclarationInfo(objectScope, expressionType)
				}
				else -> throw RuntimeException("unknown declaration type")
			}
		}

		if (mustBeConcrete) {
			objectType.signatures.symbols.values.forEach { symbol ->
				if (symbol.isAbstract) throw AnalysisError("field '${symbol.name}' not initialized", sourceDescription, ctx.start)
			}
		}

		return objectType
	}

	fun visitBlock(ctx: TinyScriptParser.BlockContext, scope: Scope): Type {
		val blockScope = Scope(scope, SignatureCollection())
		for (declaration in ctx.declaration()) {
			visitLocalDeclaration(declaration, blockScope)
		}
		return visitExpression(ctx.expression(), blockScope)
	}

	fun visitFunctionExpression(ctx: TinyScriptParser.FunctionExpressionContext, scope: Scope): DeferredType {
		val deferredType = object : DeferredType() {
			override fun createFinalType(): FinalType {
				deferredAnalyses.remove(this)

				val paramsObject: TinyScriptParser.ObjectContext? = ctx.`object`()
				val params =
						if (paramsObject != null) visitObject(paramsObject, scope, false)
						else ObjectType()

				return FunctionType(params, visitExpression(ctx.expression(), FunctionScope(scope, params)))
			}
		}
		deferredAnalyses.add(deferredType)
		return deferredType
	}

	fun visitClassExpression(ctx: TinyScriptParser.ClassExpressionContext, scope: Scope): DeferredType {
		val deferredType = object : DeferredType() {
			override fun createFinalType(): FinalType {
				deferredAnalyses.remove(this)

				return ClassType(visitObject(ctx.`object`(), scope, false))
			}
		}
		deferredAnalyses.add(deferredType)
		return deferredType
	}

	fun visitReference(nameToken: TerminalNode, lhsExpressionCtx: TinyScriptParser.ExpressionContext?, scope: Scope): Symbol {
		val name = nameToken.text

		return if (lhsExpressionCtx == null) {
			scope.resolveSymbol(name)
					?: throw AnalysisError("unresolved symbol '$name'", sourceDescription, nameToken.symbol)
		} else {
			val lhsExpressionType = visitExpression(lhsExpressionCtx, scope)
			if (lhsExpressionType !is ObjectType)
				throw AnalysisError("invalid field reference: $lhsExpressionType is not an object", sourceDescription, lhsExpressionCtx.start)

			lhsExpressionType.signatures.getSymbol(name)
					?: throw AnalysisError("unresolved field '$name'", sourceDescription, nameToken.symbol)
		}
	}

	fun visitReassignmentExpression(nameToken: TerminalNode, lhsExpressionCtx: TinyScriptParser.ExpressionContext?, rhsExpressionCtx: TinyScriptParser.ExpressionContext, scope: Scope): Type {
		val symbol = visitReference(nameToken, lhsExpressionCtx, scope)

		if (!symbol.isMutable) throw AnalysisError("symbol not reassignable", sourceDescription, nameToken.symbol)

		val finalSymbolType = symbol.type.final()
		val finalNewValueType = visitExpression(rhsExpressionCtx, scope).final()
		if (!finalSymbolType.accepts(finalNewValueType))
			throw AnalysisError("invalid reassignment: $finalSymbolType does not accept $finalNewValueType", sourceDescription, nameToken.symbol)

		return NullableType(AnyType)
	}

	fun visitExpression(ctx: TinyScriptParser.ExpressionContext, scope: Scope): Type {
		return when (ctx) {
			is TinyScriptParser.BlockExpressionContext -> visitBlock(ctx.block(), scope)
			is TinyScriptParser.IntegerLiteralExpressionContext -> intClass.simpleInstanceType
			is TinyScriptParser.FloatLiteralExpressionContext -> floatClass.simpleInstanceType
			is TinyScriptParser.StringLiteralExpressionContext -> stringClass.simpleInstanceType
			is TinyScriptParser.BooleanLiteralExpressionContext -> booleanClass.simpleInstanceType
			is TinyScriptParser.ClassExpressionContext -> visitClassExpression(ctx, scope)
			is TinyScriptParser.NullExpressionContext -> NullableType(
					if (ctx.type() != null) visitType(ctx.type(), scope) else AnyType
			)
			is TinyScriptParser.ThisExpressionContext -> {
				val objectScope: ObjectScope = ObjectScope.resolveObjectScope(scope)
						?: throw AnalysisError("not inside object scope", sourceDescription, ctx.start)

				objectScope.objectType
			}
			is TinyScriptParser.SuperExpressionContext -> {
				TODO()
			}
			is TinyScriptParser.ReferenceExpressionContext -> {
				val symbol = visitReference(ctx.Name(), null, scope)
				infoMap[ctx] = ReferenceExpressionInfo(scope, symbol.type, symbol)
				symbol.type
			}
			is TinyScriptParser.DotReferenceExpressionContext -> {
				val symbol = visitReference(ctx.Name(), ctx.expression(), scope)
				infoMap[ctx] = ReferenceExpressionInfo(scope, symbol.type, symbol)
				symbol.type
			}
			is TinyScriptParser.FunctionExpressionContext -> visitFunctionExpression(ctx, scope)
			is TinyScriptParser.ObjectExpressionContext ->
				visitObject(ctx.`object`(), scope, true)
			is TinyScriptParser.FunctionCallExpressionContext -> {
				val functionType = (visitExpression(ctx.expression(), scope).final() as? FunctionType)
						?: throw AnalysisError("must be a function", sourceDescription, ctx.start)

				val arguments = visitObject(ctx.`object`(), scope, true)
				if (!functionType.params.accepts(arguments))
					throw AnalysisError("invalid arguments", sourceDescription, ctx.`object`().start)

				infoMap[ctx] = FunctionCallExpressionInfo(scope, functionType.returnType, functionType)
				functionType.returnType
			}
			is TinyScriptParser.ReassignmentExpressionContext ->
				visitReassignmentExpression(ctx.Name(), null, ctx.expression(), scope)
			is TinyScriptParser.DotReassignmentExpressionContext ->
				visitReassignmentExpression(ctx.Name(), ctx.expression(0), ctx.expression(1), scope)
			is TinyScriptParser.PrefixOperatorCallExpressionContext -> {
				val name = ctx.Operator().text
				val rhsType = visitExpression(ctx.expression(), scope).final()
				val operator = scope.resolveOperator(name, null, rhsType)
						?: throw AnalysisError("unresolved operator '$name'", sourceDescription, ctx.start)

				infoMap[ctx] = OperatorCallExpressionInfo(scope, operator.type, operator)
				operator.type
			}
			is TinyScriptParser.InfixOperatorCallExpressionContext -> {
				val name = ctx.Operator().text
				val lhsType = visitExpression(ctx.expression(0), scope).final()
				val rhsType = visitExpression(ctx.expression(1), scope).final()
				val operator = scope.resolveOperator(name, lhsType, rhsType)
						?: throw AnalysisError("unresolved operator '$name'", sourceDescription, ctx.start)

				infoMap[ctx] = OperatorCallExpressionInfo(scope, operator.type, operator)
				operator.type
			}
			is TinyScriptParser.ConditionalExpressionContext -> {
				ctx.block().forEach {
					if (visitBlock(it, scope).final() !== booleanClass.simpleInstanceType)
						throw AnalysisError("condition must be of boolean type", sourceDescription, it.start)
				}

				ctx.expression().map { visitExpression(it, scope).final() }.reduce { acc, expressionContext ->
					intersectTypes(acc, expressionContext)
				}
			}
			else -> throw RuntimeException("unknown expression type")
		}
	}
}
