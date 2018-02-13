package tinyscript.compiler.core

import tinyscript.compiler.core.parser.TinyScriptParser
import tinyscript.compiler.util.Deferred
import java.util.*

class AnalysisException(message: String) : Throwable(message)

class DeclarationCollection(
	val scope: Scope,
	val orderedDeclarations: List<Declaration>,
	val hasImpureDeclarations: Boolean,
	val deferreds: Deferred.Collection
)

fun TinyScriptParser.DeclarationsContext.analyse(parentScope: Scope?, allowImpure: Boolean): DeclarationCollection =
	declaration().analyse(parentScope, allowImpure)

fun Iterable<TinyScriptParser.DeclarationContext>.analyse(parentScope: Scope?, allowImpure: Boolean): DeclarationCollection {
	val entityCollection = MutableEntityCollection()
	val scope = Scope(parentScope, entityCollection)
	val orderedDeclarations: LinkedList<Declaration> = LinkedList()

	var hasImpureDeclarations = false
	val deferreds = Deferred.Collection()
	forEach { declarationCtx ->
		when (declarationCtx) {
			is TinyScriptParser.NameDeclarationContext -> {
				val name: String = declarationCtx.Name().text
				val isImpure: Boolean = declarationCtx.Impure() != null
				val deferredType: Deferred<Type> = Deferred { isRoot: Boolean ->
					val initializer = declarationCtx.initializer().analyse(scope)
					if (!isImpure && initializer.expression.isImpure) {
						if (!allowImpure) throw AnalysisException("impure declaration in pure scope not allowed")
						if (!isRoot) throw AnalysisException("can not forward reference an order-sensitive declaration")
						hasImpureDeclarations = true
					}

					orderedDeclarations.add(NameDeclaration(
						name,
						isImpure,
						initializer
					))
					initializer.type
				}
				entityCollection.nameEntities.add(NameEntity(name, isImpure, deferredType))
				deferreds.add(deferredType)
			}
			is TinyScriptParser.FunctionDeclarationContext -> {
				val name: String = declarationCtx.Name().text
				val isImpure: Boolean = declarationCtx.Impure() != null
				val deferredParamsObjectType = Deferred {
					declarationCtx.objectType().analyse(scope)
				}
				val deferredType: Deferred<Type> = Deferred {
					val paramsObjectType = deferredParamsObjectType.get()

					val initializer = declarationCtx.initializer().analyse(
						Scope(scope, paramsObjectType.entityCollection)
					)

					if (!isImpure && initializer.expression.isImpure)
						throw AnalysisException("function expression must be pure if the signature is pure")

					orderedDeclarations.add(FunctionDeclaration(
						name,
						paramsObjectType,
						isImpure,
						initializer
					))
					initializer.type
				}
				entityCollection.functionEntities.add(FunctionEntity(
					name, deferredParamsObjectType, isImpure, deferredType
				))
				deferreds.add(deferredParamsObjectType)
				deferreds.add(deferredType)
			}
			is TinyScriptParser.OperatorDeclarationContext -> {
				// TODO
			}
			is TinyScriptParser.TypeAliasDeclarationContext -> {
				val name = declarationCtx.Name().text
				val deferredType = Deferred {
					declarationCtx.typeExpression().analyse(scope)
						.also { type ->
							orderedDeclarations.add(TypeAliasDeclaration(name, type))
						}
				}
				entityCollection.typeEntities.add(TypeEntity(name, deferredType))
				deferreds.add(deferredType)
			}
			is TinyScriptParser.EnumTypeDeclarationContext -> {
				// TODO
			}
			is TinyScriptParser.NonDeclarationContext -> {
				val deferredExpression = Deferred { isRoot: Boolean ->
					declarationCtx.expression().analyse(scope)
						.also { expression ->
							if (expression.isImpure) {
								if (!allowImpure) throw AnalysisException("impure declaration in pure scope not allowed")
								if (!isRoot) throw RuntimeException("this should be impossible")
								hasImpureDeclarations = true
							}
							orderedDeclarations.add(NonDeclaration(
								expression
							))
						}
				}
				deferreds.add(deferredExpression)
			}
			is TinyScriptParser.InheritDeclarationContext -> {
				// TODO
			}
			else -> throw RuntimeException("unknown declaration class")
		}
	}

	return DeclarationCollection(scope, orderedDeclarations, hasImpureDeclarations, deferreds)
}

fun TinyScriptParser.InitializerContext.analyse(scope: Scope): Initializer {
	val explicitType: Type? = typeExpression()?.analyse(scope)
	val expression = expression().analyse(scope)

	explicitType?.let {
		if (!it.accepts(expression.type))
			throw AnalysisException("explicit type does not accept expression type")
	}

	return Initializer(explicitType, expression)
}

fun TinyScriptParser.ExpressionContext.analyse(scope: Scope): Expression = when (this) {
	is TinyScriptParser.BlockExpressionContext ->
		block().analyse(scope)
	is TinyScriptParser.IntegerLiteralExpressionContext ->
		IntExpression(text.toInt())
	is TinyScriptParser.FloatLiteralExpressionContext ->
		FloatExpression(text.toDouble())
	is TinyScriptParser.StringLiteralExpressionContext -> TODO()
	is TinyScriptParser.BooleanLiteralExpressionContext -> TODO()
	is TinyScriptParser.NullExpressionContext ->
		NullExpression(typeExpression()?.analyse(scope) ?: AnyType)
	is TinyScriptParser.ReferenceExpressionContext -> {
		val name: String = Name().text
		val isImpure = Impure() != null
		val nameEntity: NameEntity = scope.findNameEntity(name, isImpure)
			?: throw AnalysisException("unresolved reference")
		ReferenceExpression(
			name,
			isImpure,
			nameEntity.deferredType.get()
		)
	}
	is TinyScriptParser.ObjectExpressionContext ->
		`object`().analyse(scope)
	is TinyScriptParser.FunctionCallExpressionContext -> {
		val name: String = Name().text
		val isImpure = Impure() != null
		val argumentsObjectExpression = `object`().analyse(scope)
		val functionEntity: FunctionEntity = scope.findFunctionEntity(name, argumentsObjectExpression.type, isImpure)
			?: throw AnalysisException("unresolved reference")
		FunctionCallExpression(
			name,
			argumentsObjectExpression,
			isImpure,
			functionEntity.deferredType.get()
		)
	}
	else -> TODO()
}

fun TinyScriptParser.BlockContext.analyse(scope: Scope): BlockExpression {
	val declarationCollection = declarations()?.analyse(scope, true)
	return BlockExpression(declarationCollection, expression().analyse(declarationCollection?.scope ?: scope))
}

fun TinyScriptParser.ObjectContext.analyse(scope: Scope): ObjectExpression {
	val declarationCollection = declarations()?.analyse(scope, true)
	return ObjectExpression(declarationCollection)
}

fun TinyScriptParser.TypeExpressionContext.analyse(scope: Scope): Type = when (this) {
	is TinyScriptParser.ParenTypeExpressionContext -> typeExpression().analyse(scope)
	is TinyScriptParser.FunctionTypeExpressionContext -> FunctionType(
		objectType()?.analyse(scope) ?: ObjectType(EmptyEntityCollection, emptySet()),
		typeExpression().analyse(scope)
	)
	is TinyScriptParser.NullTypeExpressionContext -> NullableType(AnyType)
	is TinyScriptParser.NullableTypeExpressionContext -> NullableType(typeExpression().analyse(scope))
	is TinyScriptParser.ObjectTypeExpressionContext -> objectType().analyse(scope)
	is TinyScriptParser.TypeReferenceExpressionContext -> {
		val name: String = Name().text
		val typeEntity: TypeEntity = scope.findTypeEntity(name)
			?: throw AnalysisException("unresolved reference '$name'")
		typeEntity.deferredType.get()
	}
	else -> TODO()
}

fun TinyScriptParser.ObjectTypeContext.analyse(parentScope: Scope): ObjectType {
	val entityCollection = MutableEntityCollection()
	val scope = Scope(parentScope, entityCollection)

	val deferreds = Deferred.Collection()
	objectTypeField().forEach { objectTypeFieldCtx ->
		when (objectTypeFieldCtx) {
			is TinyScriptParser.NameObjectTypeFieldContext -> {
				val name: String = objectTypeFieldCtx.Name().text
				val isImpure: Boolean = objectTypeFieldCtx.Impure() != null
				val deferredType: Deferred<Type> = Deferred {
					objectTypeFieldCtx.typeExpression().analyse(scope)
				}
				entityCollection.nameEntities.add(NameEntity(name, isImpure, deferredType))
				deferreds.add(deferredType)
			}
			is TinyScriptParser.FunctionObjectTypeFieldContext -> {
				val name: String = objectTypeFieldCtx.Name().text
				val isImpure: Boolean = objectTypeFieldCtx.Impure() != null
				val deferredParamsObjectType = Deferred {
					objectTypeFieldCtx.objectType().analyse(scope)
				}
				val deferredType: Deferred<Type> = Deferred {
					objectTypeFieldCtx.typeExpression().analyse(scope)
				}
				entityCollection.functionEntities.add(FunctionEntity(
					name, deferredParamsObjectType, isImpure, deferredType
				))
				deferreds.add(deferredParamsObjectType)
				deferreds.add(deferredType)
			}
			else -> TODO()
		}
	}

	return ObjectType(entityCollection, emptySet())
}
