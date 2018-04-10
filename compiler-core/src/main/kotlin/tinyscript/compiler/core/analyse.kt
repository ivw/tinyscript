package tinyscript.compiler.core

import tinyscript.compiler.core.parser.TinyScriptParser
import tinyscript.compiler.scope.*
import tinyscript.compiler.util.SafeLazy

class AnalysisException(message: String) : Throwable(message)

class StatementCollection(
	val scope: Scope,
	val orderedStatements: List<Statement>
)

fun TinyScriptParser.StatementListContext.analyse(parentScope: Scope?): StatementCollection =
	statement().analyse(parentScope)

fun Iterable<TinyScriptParser.StatementContext>.analyse(parentScope: Scope?): StatementCollection {
	val entityCollection = MutableEntityCollection()
	val scope = Scope(parentScope, entityCollection)
	val orderedStatements: MutableList<Statement> = ArrayList()

	// first make sure all the type entities are in the scope
	forEach { statementCtx ->
		when (statementCtx) {
			is TinyScriptParser.TypeAliasDeclarationContext -> {
				val name = statementCtx.Name().text
				val typeAliasDeclaration = TypeAliasDeclaration(
					name,
					SafeLazy { statementCtx.typeExpression().analyse(scope) }
				)
				entityCollection.typeEntities.add(TypeEntity(
					name,
					{ typeAliasDeclaration.lazyTypeExpression.get().type }
				))
				orderedStatements.add(typeAliasDeclaration)
			}
		}
	}

	// now add all the value entities to scope (some entity signatures need the type entities)
	val lazyImperativeStatements: MutableList<SafeLazy<ImperativeStatement>> = ArrayList()
	forEach { statementCtx ->
		when (statementCtx) {
			is TinyScriptParser.ImperativeStatementContext -> {
				val name: String? = statementCtx.Name()?.text

				val lazyImperativeStatement = SafeLazy { isRoot ->
					val expression = statementCtx.expression().analyse(scope)
					if (!isRoot && expression.isImpure)
						throw AnalysisException("can not forward reference an impure declaration")

					val imperativeStatement = ImperativeStatement(name, expression)
					orderedStatements.add(imperativeStatement)
					imperativeStatement
				}

				if (name != null) {
					entityCollection.valueEntities.add(ValueEntity(
						NameSignature(null, name, false, null),
						{ lazyImperativeStatement.get().expression.type }
					))
				}
				lazyImperativeStatements.add(lazyImperativeStatement)
			}
			is TinyScriptParser.FunctionDeclarationContext -> {
				val signatureExpression = statementCtx.signature().analyse(scope)
				val functionDeclaration = FunctionDeclaration(signatureExpression, SafeLazy {
					val expression = statementCtx.expression().analyse(scope)

					if (expression.isImpure && !signatureExpression.signature.isImpure)
						throw AnalysisException("function signature must be impure if its expression is impure")

					expression
				})
				entityCollection.valueEntities.add(ValueEntity(
					signatureExpression.signature,
					{ functionDeclaration.lazyExpression.get().type }
				))
				orderedStatements.add(functionDeclaration)
			}
			else -> throw RuntimeException("unknown statement class")
		}
	}

	// lastly, analyse all imperative statements
	lazyImperativeStatements.forEach { it.get(true) }

	return StatementCollection(scope, orderedStatements)
}

fun TinyScriptParser.SignatureContext.analyse(scope: Scope): SignatureExpression = when (this) {
	is TinyScriptParser.NameSignatureContext -> NameSignatureExpression(
		typeExpression()?.analyse(scope),
		Name().text,
		Impure() != null,
		objectType()?.analyse(scope)
	)
	is TinyScriptParser.OperatorSignatureContext -> OperatorSignatureExpression(
		lhs?.analyse(scope),
		OperatorSymbol().text,
		Impure() != null,
		rhs.analyse(scope)
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
	is TinyScriptParser.NameReferenceExpressionContext -> {
		val name: String = Name().text
		val isImpure: Boolean = Impure() != null
		val argumentsObjectExpression: ObjectExpression? = `object`()?.analyse(scope)
		val valueEntity: ValueEntity = scope.findValueEntity(NameSignature(
			null,
			name,
			isImpure,
			argumentsObjectExpression?.type
		))
			?: throw AnalysisException("unresolved reference")
		NameReferenceExpression(
			name,
			isImpure,
			argumentsObjectExpression,
			valueEntity.getType()
		)
	}
	is TinyScriptParser.ObjectExpressionContext ->
		`object`().analyse(scope)
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
	is TinyScriptParser.ObjectTypeExpressionContext -> objectType().analyse(scope)
	is TinyScriptParser.TypeReferenceExpressionContext -> {
		val name: String = Name().text
		val typeEntity: TypeEntity = scope.findTypeEntity(name)
			?: throw AnalysisException("unresolved reference '$name'")
		typeEntity.deferredType.get()
	}
	else -> TODO()
}

fun TinyScriptParser.ObjectTypeContext.analyse(parentScope: Scope): ObjectType = TODO()
