package tinyscript.compiler.core

import tinyscript.compiler.core.parser.TinyScriptParser
import tinyscript.compiler.util.SafeLazy
import java.util.*

class AnalysisException(message: String) : Throwable(message)

class StatementCollection(
	val scope: Scope,
	val orderedStatements: List<Statement>,
	val hasImpureStatements: Boolean
)

fun TinyScriptParser.StatementListContext.analyse(parentScope: Scope?, allowImpure: Boolean): StatementCollection =
	statement().analyse(parentScope, allowImpure)

fun Iterable<TinyScriptParser.StatementContext>.analyse(parentScope: Scope?, allowImpure: Boolean): StatementCollection {
	val entityCollection = MutableEntityCollection()
	val scope = Scope(parentScope, entityCollection)
	val orderedStatements: LinkedList<Statement> = LinkedList()

	var hasImpureStatements = false
	forEach { declarationCtx ->
		when (declarationCtx) {
			is TinyScriptParser.ValueDeclarationContext -> {
				val signature = declarationCtx.signature().analyse(scope)
			}

			is TinyScriptParser.NameDeclarationContext -> {
				val name: String = declarationCtx.Name().text
				val isImpure: Boolean = declarationCtx.Impure() != null
				val deferredType: Deferred<Type> = Deferred { isRoot: Boolean ->
					val expression = declarationCtx.expression().analyse(scope)
					if (!isImpure && expression.isImpure) {
						if (!allowImpure) throw AnalysisException("impure declaration in pure scope not allowed")
						if (!isRoot) throw AnalysisException("can not forward reference an order-sensitive declaration")
						hasImpureStatements = true
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
								hasImpureStatements = true
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

	return StatementCollection(scope, orderedStatements, hasImpureStatements)
}

fun TinyScriptParser.SignatureContext.analyse(scope: Scope): Signature = when (this) {
	is TinyScriptParser.NameSignatureContext -> NameSignature(
		Name().text,
		Impure() != null,
		objectType()?.let { objectTypeCtx ->
			SafeLazy { objectTypeCtx.analyse(scope) }
		}
	)
	else -> throw RuntimeException("unknown signature class")
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

fun TinyScriptParser.TypeExpressionContext.analyse(scope: Scope): TypeExpression = when (this) {
	is TinyScriptParser.ParenTypeExpressionContext -> ParenTypeExpression(typeExpression().analyse(scope))
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
