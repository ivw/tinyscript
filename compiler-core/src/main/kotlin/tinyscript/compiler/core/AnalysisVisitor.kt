package tinyscript.compiler.core

import org.antlr.v4.runtime.tree.TerminalNode
import tinyscript.compiler.core.parser.TinyScriptParser
import java.nio.file.Path
import java.util.*
import kotlin.collections.LinkedHashMap

class AnalysisResult(val scope: Scope, val type: Type)

class AnalysisVisitor(val filePath: Path) {
	val resultMap: MutableMap<TinyScriptParser.ExpressionContext, AnalysisResult> = HashMap()

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
		val scope = GlobalScope()
		for (declaration in ctx.declaration()) {
			val symbol = visitDeclaration(declaration, scope)

			if (symbol.isAbstract) throw AnalysisError("concrete declaration expected", filePath, declaration.start)

			scope.defineSymbol(symbol)
		}
	}

	fun visitDeclaration(ctx: TinyScriptParser.DeclarationContext, scope: Scope): Symbol {
		return when (ctx) {
			is TinyScriptParser.AbstractDeclarationContext -> {
				visitSymbol(ctx.symbol(), visitType(ctx.type(), scope), true)
			}
			is TinyScriptParser.ConcreteDeclarationContext -> {
				// if it has a rhs expression, it must be visited
				val expressionType = visitExpression(ctx.expression(), scope)

				val typeAnnotationType: FinalType? = ctx.type()?.let { visitType(it, scope) }

				val type: Type = typeAnnotationType ?: expressionType
				val symbol = visitSymbol(ctx.symbol(), type, false)

				// when assigning a DeferredFunctionType to a declaration with type annotation, the function must be analysed.
				// TODO this should somehow be done after defining the symbol, so that recursive functions with type annotations are possible?
				if (typeAnnotationType != null) {
					val finalExpressionType = expressionType.final()
					if (!typeAnnotationType.accepts(finalExpressionType))
						throw AnalysisError("invalid value for symbol '${symbol.name}': $typeAnnotationType does not accept $finalExpressionType", filePath, ctx.start)
				}

				println("Type of symbol '${symbol.name}' is $type")
				symbol
			}
			is TinyScriptParser.ImplicitDeclarationContext -> throw RuntimeException("invalid implicit declaration")
			else -> throw RuntimeException("unknown declaration type")
		}
	}

	fun visitSymbol(ctx: TinyScriptParser.SymbolContext, type: Type, isAbstract: Boolean): Symbol {
		val name = ctx.Name().text

		return Symbol(
				name,
				type,
				isAbstract,
				ctx.getToken(TinyScriptParser.Private, 0) != null,
				ctx.getToken(TinyScriptParser.Override, 0) != null,
				ctx.getToken(TinyScriptParser.Hash, 0) != null
		)
	}

	fun visitType(ctx: TinyScriptParser.TypeContext, scope: Scope): FinalType {
		return when (ctx) {
			is TinyScriptParser.ParenTypeContext -> visitType(ctx.type(), scope)
			is TinyScriptParser.FunctionTypeContext -> {
				val paramsObjectType: TinyScriptParser.ObjectTypeContext? = ctx.objectType()
				val params =
						if (paramsObjectType != null) visitObjectType(paramsObjectType, scope)
						else ObjectType(false)
				FunctionType(params, visitType(ctx.type(), scope))
			}
			is TinyScriptParser.NullTypeContext -> NullableType(AnyType)
			is TinyScriptParser.NullableTypeContext -> NullableType(visitType(ctx.type(), scope))
			is TinyScriptParser.ObjectTypeTypeContext -> visitObjectType(ctx.objectType(), scope)
			is TinyScriptParser.TypeReferenceContext -> {
				val referenceSymbol = scope.resolveSymbolOrFail(ctx.Name().text)
				val classType = referenceSymbol.type.final() as ClassType
				classType.objectType
			}
			is TinyScriptParser.UnionObjectTypeContext -> {
				val leftType = visitType(ctx.type(0), scope) as ObjectType
				val rightType = visitType(ctx.type(1), scope) as ObjectType
				unionObjectType(leftType, rightType)
			}
			is TinyScriptParser.IntersectObjectTypeContext -> {
				val leftType = visitType(ctx.type(0), scope) as ObjectType
				val rightType = visitType(ctx.type(1), scope) as ObjectType
				intersectObjectType(leftType, rightType)
			}
			else -> throw RuntimeException("unknown TypeContext type")
		}
	}

	fun visitObjectType(ctx: TinyScriptParser.ObjectTypeContext, scope: Scope): ObjectType {
		val symbols: LinkedHashMap<String, Symbol> = LinkedHashMap()
		val objectType = ObjectType(false, emptySet(), symbols)
		val objectScope = ObjectScope(scope, objectType)
		for (field in ctx.objectTypeField()) {
			val symbol = visitSymbol(field.symbol(), visitType(field.type(), objectScope), true)

			if (symbols.containsKey(symbol.name))
				throw AnalysisError("name already exists in this object", filePath, field.start)

			symbols[symbol.name] = symbol
		}
		return objectType
	}

	fun visitObject(ctx: TinyScriptParser.ObjectContext, scope: Scope, isNominal: Boolean, superObjectType: ObjectType?, mustBeConcrete: Boolean): ObjectType {
		val symbols: LinkedHashMap<String, Symbol> =
				if (superObjectType != null) LinkedHashMap(superObjectType.symbols) else LinkedHashMap()
		val objectType = ObjectType(isNominal, superObjectType?.identities ?: emptySet(), symbols)
		val objectScope = ObjectScope(scope, objectType)

		var superSymbolsIterator = superObjectType?.let { it.symbols.values.iterator() }

		for (declaration in ctx.declaration()) {
			val symbol: Symbol = if (declaration is TinyScriptParser.ImplicitDeclarationContext) {
				if (superSymbolsIterator == null || !superSymbolsIterator.hasNext())
					throw AnalysisError("invalid implicit declaration", filePath, declaration.start)

				val superSymbol: Symbol = superSymbolsIterator.next()
				val expressionType = visitExpression(declaration.expression(), scope)
				Symbol(superSymbol.name, expressionType, false, superSymbol.isPrivate, true, superSymbol.isMutable)
			} else {
				superSymbolsIterator = null
				visitDeclaration(declaration, objectScope)
			}

//			if (symbols.containsKey(symbol.name))
//				throw AnalysisError("name already exists in this object", declaration.start)
			// TODO find something for this. it's not critical, though

			symbols[symbol.name]?.let { superSymbol ->
				if (!superSymbol.type.final().accepts(symbol.type.final()))
					throw AnalysisError("incompatible override on field '${symbol.name}': ${superSymbol.type} does not accept ${symbol.type}", filePath, declaration.start)
			}

			symbols[symbol.name] = symbol
		}

		if (mustBeConcrete) {
			symbols.values.forEach { symbol ->
				if (symbol.isAbstract) throw AnalysisError("field '${symbol.name}' not initialized", filePath, ctx.start)
			}
		}

		return objectType
	}

	fun visitBlock(ctx: TinyScriptParser.BlockContext, scope: Scope): Type {
		val blockScope = LocalScope(scope)
		for (declaration in ctx.declaration()) {
			if (declaration is TinyScriptParser.ImplicitDeclarationContext) {
				// local implicit declarations define no symbol. nothing is done with the expression value, but it is still checked.
				visitExpression(declaration.expression(), blockScope)
			} else {
				val symbol = visitDeclaration(declaration, blockScope)
				blockScope.defineSymbol(symbol)
			}
		}
		return visitExpression(ctx.expression(), blockScope)
	}

	fun visitObjectInstanceExpression(superObjectType: ObjectType?, argsObjectCtx: TinyScriptParser.ObjectContext, scope: Scope): Type {
		val instanceObjectType = visitObject(argsObjectCtx, scope, false, superObjectType, true)
		return instanceObjectType
	}

	fun visitFunctionCallExpression(functionType: FunctionType, argsObjectCtx: TinyScriptParser.ObjectContext, scope: Scope): Type {
		val argumentsObjectType = visitObject(argsObjectCtx, scope, false, functionType.params, true)
		return functionType.returnType
	}

	fun visitFunctionExpression(ctx: TinyScriptParser.FunctionExpressionContext, scope: Scope): DeferredType {
		val deferredType = object : DeferredType() {
			override fun createFinalType(): FinalType {
				deferredAnalyses.remove(this)

				val paramsObject: TinyScriptParser.ObjectContext? = ctx.`object`()
				val params =
						if (paramsObject != null) visitObject(paramsObject, scope, false, null, false)
						else ObjectType(false)

				return FunctionType(params, visitExpression(ctx.expression(), FunctionScope(scope, params)))
			}
		}
		deferredAnalyses.add(deferredType)
		return deferredType
	}

	fun visitClassExpression(lhsExpressionCtx: TinyScriptParser.ExpressionContext?, objectCtx: TinyScriptParser.ObjectContext, scope: Scope): DeferredType {
		val deferredType = object : DeferredType() {
			override fun createFinalType(): FinalType {
				deferredAnalyses.remove(this)

				val superType = lhsExpressionCtx?.let {
					(visitExpression(it, scope).final() as ClassType).objectType
				}

				return ClassType(visitObject(objectCtx, scope, true, superType, false))
			}
		}
		deferredAnalyses.add(deferredType)
		return deferredType
	}

	fun visitReference(nameToken: TerminalNode, lhsExpressionCtx: TinyScriptParser.ExpressionContext?, scope: Scope): Symbol {
		val name = nameToken.text

		if (lhsExpressionCtx == null) {
			return scope.resolveSymbolOrFail(name)
		}

		val lhsExpressionType = visitExpression(lhsExpressionCtx, scope)
		if (lhsExpressionType !is ObjectType)
			throw AnalysisError("invalid field reference: $lhsExpressionType is not an object", filePath, lhsExpressionCtx.start)

		return lhsExpressionType.symbols[name] ?: throw AnalysisError("unresolved field '$name'", filePath, nameToken.symbol)
	}

	fun visitReassignmentExpression(nameToken: TerminalNode, lhsExpressionCtx: TinyScriptParser.ExpressionContext?, rhsExpressionCtx: TinyScriptParser.ExpressionContext, scope: Scope): Type {
		val symbol = visitReference(nameToken, lhsExpressionCtx, scope)

		if (!symbol.isMutable) throw AnalysisError("symbol not reassignable", filePath, nameToken.symbol)

		val finalSymbolType = symbol.type.final()
		val finalNewValueType = visitExpression(rhsExpressionCtx, scope).final()
		if (!finalSymbolType.accepts(finalNewValueType))
			throw AnalysisError("invalid reassignment: $finalSymbolType does not accept $finalNewValueType", filePath, nameToken.symbol)

		return NullableType(AnyType)
	}

	fun visitExpression(ctx: TinyScriptParser.ExpressionContext, scope: Scope): Type {
		val type: Type = when (ctx) {
			is TinyScriptParser.BlockExpressionContext -> visitBlock(ctx.block(), scope)
			is TinyScriptParser.IntegerLiteralExpressionContext -> intClass.objectType
			is TinyScriptParser.FloatLiteralExpressionContext -> floatClass.objectType
			is TinyScriptParser.StringLiteralExpressionContext -> stringClass.objectType
			is TinyScriptParser.BooleanLiteralExpressionContext -> booleanClass.objectType
			is TinyScriptParser.ClassExpressionContext -> visitClassExpression(null, ctx.`object`(), scope)
			is TinyScriptParser.ExtendClassExpressionContext -> visitClassExpression(ctx.expression(), ctx.`object`(), scope)
			is TinyScriptParser.ClassMergeExpressionContext -> {
				val lhsClassType = visitExpression(ctx.expression(0), scope).final() as ClassType
				val rhsClassType = visitExpression(ctx.expression(1), scope).final() as ClassType
				ClassType(unionObjectType(lhsClassType.objectType, rhsClassType.objectType))
			}
			is TinyScriptParser.NullExpressionContext -> NullableType(
					if (ctx.type() != null) visitType(ctx.type(), scope) else AnyType
			)
			is TinyScriptParser.ThisExpressionContext -> {
				val objectScope: ObjectScope = ObjectScope.resolveObjectScope(scope)
						?: throw AnalysisError("not inside object scope", filePath, ctx.start)

				objectScope.objectType
			}
			is TinyScriptParser.SuperExpressionContext -> {
				val objectScope: ObjectScope = ObjectScope.resolveObjectScope(scope)
						?: throw AnalysisError("not inside object scope", filePath, ctx.start)

//				objectScope.objectType.superObjectType
//						?: throw AnalysisError("no super object type", filePath, ctx.start)
				TODO()
			}
			is TinyScriptParser.ReferenceExpressionContext -> visitReference(ctx.Name(), null, scope).type
			is TinyScriptParser.DotReferenceExpressionContext -> visitReference(ctx.Name(), ctx.expression(), scope).type
			is TinyScriptParser.FunctionExpressionContext -> visitFunctionExpression(ctx, scope)
			is TinyScriptParser.ObjectExpressionContext ->
				visitObjectInstanceExpression(null, ctx.`object`(), scope)
			is TinyScriptParser.ObjectOrCallExpressionContext -> {
				val finalClassOrFunctionType: FinalType = visitExpression(ctx.expression(), scope).final()
				return when (finalClassOrFunctionType) {
					is FunctionType -> visitFunctionCallExpression(finalClassOrFunctionType, ctx.`object`(), scope)
					is ClassType -> visitObjectInstanceExpression(finalClassOrFunctionType, ctx.`object`(), scope)
					else -> throw AnalysisError("can only call a function or a class", filePath, ctx.start)
				}
			}
			is TinyScriptParser.ReassignmentExpressionContext ->
				visitReassignmentExpression(ctx.Name(), null, ctx.expression(), scope)
			is TinyScriptParser.DotReassignmentExpressionContext ->
				visitReassignmentExpression(ctx.Name(), ctx.expression(0), ctx.expression(1), scope)
			is TinyScriptParser.PrefixOperatorCallExpressionContext -> {
				visitExpression(ctx.expression(), scope)
				AnyType
			}
			is TinyScriptParser.InfixOperatorCallExpressionContext -> {
				visitExpression(ctx.expression(0), scope)
				visitExpression(ctx.expression(1), scope)
				AnyType
			}
			is TinyScriptParser.ConditionalExpressionContext -> {
				ctx.block().forEach {
					if (visitBlock(it, scope).final() !== booleanClass.objectType)
						throw AnalysisError("condition must be of boolean type", filePath, it.start)
				}

				ctx.expression().map { visitExpression(it, scope).final() }.reduce { acc, expressionContext ->
					intersectTypes(acc, expressionContext)
				}
			}
			else -> throw RuntimeException("unknown expression type")
		}
		resultMap[ctx] = AnalysisResult(scope, type)
		return type
	}
}
