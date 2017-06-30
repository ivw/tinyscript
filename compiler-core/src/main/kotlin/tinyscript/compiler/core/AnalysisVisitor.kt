package tinyscript.compiler.core

import org.antlr.v4.runtime.ParserRuleContext
import tinyscript.compiler.core.parser.TinyScriptParser
import java.util.*
import kotlin.collections.LinkedHashMap

class AnalysisVisitor {
	val typeMap: MutableMap<ParserRuleContext, Type> = HashMap()

	val deferredAnalyses: MutableList<DeferredType> = LinkedList()

	fun finishDeferredAnalyses() {
		deferredAnalyses.forEach { it.final() }
	}

	fun visitFile(ctx: TinyScriptParser.FileContext) {
		val scope = GlobalScope()
		for (declaration in ctx.declaration()) {
			val symbol = visitDeclaration(declaration, scope, null)

			if (symbol.isAbstract) throw RuntimeException("concrete declaration expected")

			scope.defineSymbol(symbol)
		}
	}

	fun visitDeclaration(ctx: TinyScriptParser.DeclarationContext, scope: Scope, superSymbol: Symbol?): Symbol {
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
						throw RuntimeException("invalid value for symbol '${symbol.name}': $typeAnnotationType does not accept $finalExpressionType")
				}

				println("Type of symbol '${symbol.name}' is $type")
				symbol
			}
			is TinyScriptParser.ImplicitDeclarationContext -> {
				val expressionType = visitExpression(ctx.expression(), scope)
				if (superSymbol == null) throw RuntimeException("invalid implicit declaration")
				Symbol(superSymbol.name, expressionType, false, superSymbol.isPrivate, true, superSymbol.isMutable)
			}
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
						else ObjectType(false, objectType)
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
			is TinyScriptParser.MergedObjectTypeContext -> {
				val leftType = visitType(ctx.type(0), scope) as ObjectType
				val rightType = visitType(ctx.type(1), scope) as ObjectType
				MergeObjectType(leftType, rightType)
			}
			else -> throw RuntimeException("unknown TypeContext type")
		}
	}

	fun visitObjectType(ctx: TinyScriptParser.ObjectTypeContext, scope: Scope): ObjectType {
		val symbols: LinkedHashMap<String, Symbol> = LinkedHashMap()
		val objectType = ObjectType(false, null, symbols)
		val objectScope = ObjectScope(scope, objectType)
		for (field in ctx.objectTypeField()) {
			val symbol = visitSymbol(field.symbol(), visitType(field.type(), objectScope), true)

			if (symbols.containsKey(symbol.name))
				throw RuntimeException("name already exists in this object")

			symbols[symbol.name] = symbol
		}
		return objectType
	}

	fun visitObject(ctx: TinyScriptParser.ObjectContext, scope: Scope, isNominal: Boolean, superObjectType: ObjectType?): ObjectType {
		val symbols: LinkedHashMap<String, Symbol> =
				if (superObjectType != null) LinkedHashMap(superObjectType.symbols) else LinkedHashMap()
		val objectType = ObjectType(isNominal, superObjectType, symbols)
		val objectScope = ObjectScope(scope, objectType)
		for (declaration in ctx.declaration()) {
			val symbol = visitDeclaration(declaration, objectScope, null /* TODO */)

//			if (symbols.containsKey(symbol.name))
//				throw RuntimeException("name already exists in this object")
			// TODO find something for this. it's not critical, though

			symbols[symbol.name]?.let { superSymbol ->
				if (!superSymbol.type.final().accepts(symbol.type.final()))
					throw RuntimeException("incompatible override on field '${symbol.name}': ${superSymbol.type} does not accept ${symbol.type}")
			}

			symbols[symbol.name] = symbol
		}
		return objectType
	}

	fun visitBlock(ctx: TinyScriptParser.BlockContext, scope: Scope): Type {
		val blockScope = Scope(scope)
		for (declaration in ctx.declaration()) {
			if (declaration is TinyScriptParser.ImplicitDeclarationContext) {
				// local implicit declarations define no symbol. nothing is done with the expression value, but it is still checked.
				visitExpression(declaration.expression(), blockScope)
			} else {
				val symbol = visitDeclaration(declaration, blockScope, null)
				blockScope.defineSymbol(symbol)
			}
		}
		return visitExpression(ctx.expression(), blockScope)
	}

	fun visitObjectInstanceExpression(classType: ClassType, argsObjectCtx: TinyScriptParser.ObjectContext, scope: Scope): Type {
		val instanceObjectType = visitObject(argsObjectCtx, scope, false, classType.objectType)
		instanceObjectType.checkConcrete()
		return instanceObjectType
	}

	fun visitFunctionCallExpression(functionType: FunctionType, argsObjectCtx: TinyScriptParser.ObjectContext, scope: Scope): Type {
		val argumentsObjectType = visitObject(argsObjectCtx, scope, false, functionType.params)
		argumentsObjectType.checkConcrete()
		return functionType.returnType
	}

	fun visitFunctionExpression(ctx: TinyScriptParser.FunctionExpressionContext, scope: Scope): DeferredType {
		val deferredType = object : DeferredType() {
			override fun createFinalType(): FinalType {
				val paramsObject: TinyScriptParser.ObjectContext? = ctx.`object`()
				val params =
						if (paramsObject != null) visitObject(paramsObject, scope, false, null)
						else ObjectType(false, objectType)

				return FunctionType(params, visitExpression(ctx.expression(), FunctionScope(scope, params)))
			}
		}
		deferredAnalyses.add(deferredType)
		return deferredType
	}

	fun visitClassExpression(lhsExpressionCtx: TinyScriptParser.ExpressionContext?, objectCtx: TinyScriptParser.ObjectContext, scope: Scope): DeferredType {
		val deferredType = object : DeferredType() {
			override fun createFinalType(): FinalType {
				val superClassType = if (lhsExpressionCtx != null)
					visitExpression(lhsExpressionCtx, scope).final() as ClassType
				else objectClass

				return ClassType(visitObject(objectCtx, scope, true, superClassType.objectType))
			}
		}
		deferredAnalyses.add(deferredType)
		return deferredType
	}

	fun visitReference(name: String, lhsExpressionCtx: TinyScriptParser.ExpressionContext?, scope: Scope): Symbol {
		if (lhsExpressionCtx == null) {
			return scope.resolveSymbolOrFail(name)
		}

		val lhsExpressionType = visitExpression(lhsExpressionCtx, scope)
		if (lhsExpressionType !is ObjectType)
			throw RuntimeException("invalid field reference: $lhsExpressionType is not an object")

		return lhsExpressionType.symbols[name] ?: throw RuntimeException("unresolved field '$name'")
	}

	fun visitReassignmentExpression(symbol: Symbol, expressionCtx: TinyScriptParser.ExpressionContext, scope: Scope): Type {
		if (!symbol.isMutable) throw RuntimeException("symbol not reassignable")

		val finalSymbolType = symbol.type.final()
		val finalNewValueType = visitExpression(expressionCtx, scope).final()
		if (!finalSymbolType.accepts(finalNewValueType))
			throw RuntimeException("invalid reassignment: $finalSymbolType does not accept $finalNewValueType")

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
				ClassType(MergeObjectType(lhsClassType.objectType, rhsClassType.objectType))
			}
			is TinyScriptParser.NullExpressionContext -> NullableType(
					if (ctx.type() != null) visitType(ctx.type(), scope) else AnyType
			)
			is TinyScriptParser.ThisExpressionContext -> {
				val objectScope: ObjectScope = ObjectScope.resolveObjectScope(scope)
						?: throw RuntimeException("not inside object scope")

				objectScope.objectType
			}
			is TinyScriptParser.SuperExpressionContext -> {
				val objectScope: ObjectScope = ObjectScope.resolveObjectScope(scope)
						?: throw RuntimeException("not inside object scope")

				objectScope.objectType.superObjectType
						?: throw RuntimeException("no super object type")
			}
			is TinyScriptParser.ReferenceExpressionContext -> visitReference(ctx.Name().text, null, scope).type
			is TinyScriptParser.DotReferenceExpressionContext -> visitReference(ctx.Name().text, ctx.expression(), scope).type
			is TinyScriptParser.FunctionExpressionContext -> visitFunctionExpression(ctx, scope)
			is TinyScriptParser.ObjectExpressionContext ->
				visitObjectInstanceExpression(objectClass, ctx.`object`(), scope)
			is TinyScriptParser.ObjectOrCallExpressionContext -> {
				val finalClassOrFunctionType: FinalType = visitExpression(ctx.expression(), scope).final()
				return when (finalClassOrFunctionType) {
					is FunctionType -> visitFunctionCallExpression(finalClassOrFunctionType, ctx.`object`(), scope)
					is ClassType -> visitObjectInstanceExpression(finalClassOrFunctionType, ctx.`object`(), scope)
					else -> throw RuntimeException("can only call a function or a class")
				}
			}
			is TinyScriptParser.ReassignmentExpressionContext -> visitReassignmentExpression(
					visitReference(ctx.Name().text, null, scope),
					ctx.expression(),
					scope
			)
			is TinyScriptParser.DotReassignmentExpressionContext -> visitReassignmentExpression(
					visitReference(ctx.Name().text, ctx.expression(0), scope),
					ctx.expression(1),
					scope
			)
			is TinyScriptParser.PrefixOperatorCallExpressionContext -> objectType // TODO
			is TinyScriptParser.InfixOperatorCallExpressionContext -> objectType // TODO
			is TinyScriptParser.ConditionalExpressionContext -> objectType // TODO
			else -> throw RuntimeException("unknown expression type")
		}
		typeMap.put(ctx, type)
		return type
	}
}
