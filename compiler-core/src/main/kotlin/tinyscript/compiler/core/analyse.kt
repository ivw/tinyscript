package tinyscript.compiler.core

import tinyscript.compiler.core.parser.TinyScriptParser
import tinyscript.compiler.scope.*
import tinyscript.compiler.util.SafeLazy

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
	val statements: List<Statement> = map { statementCtx ->
		when (statementCtx) {
			is TinyScriptParser.ValueDeclarationContext -> {
				val signatureExpression = statementCtx.signature().analyse(scope)
				val valueDeclaration = ValueDeclaration(signatureExpression, SafeLazy {
					val expression = statementCtx.expression().analyse(scope)

					if (expression.isImpure && signatureExpression.signature.isImpure) throw AnalysisException()

					expression
				})
				entityCollection.valueEntities.add(ValueEntity(signatureExpression.signature, {
					valueDeclaration.lazyExpression.get().type
				}))
				valueDeclaration
			}
			else -> throw RuntimeException("unknown statement class")
		}
	}

	return StatementCollection(scope, orderedStatements, hasImpureStatements)
}

fun TinyScriptParser.SignatureContext.analyse(scope: Scope): SignatureExpression = when (this) {
	is TinyScriptParser.NameSignatureContext -> NameSignatureExpression(
		typeExpression()?.let { typeExpressionCtx ->
			SafeLazy { typeExpressionCtx.analyse(scope) }
		},
		Name().text,
		Impure() != null,
		objectType()?.let { objectTypeCtx ->
			SafeLazy { objectTypeCtx.analyse(scope) }
		}
	)
	is TinyScriptParser.OperatorSignatureContext -> OperatorSignatureExpression(
		lhs?.let { typeExpressionCtx -> SafeLazy { typeExpressionCtx.analyse(scope) } },
		OperatorSymbol().text,
		Impure() != null,
		SafeLazy { rhs.analyse(scope) }
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
		objectType()?.analyse(scope)
			?: ObjectType(EmptyEntityCollection, emptySet()),
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
