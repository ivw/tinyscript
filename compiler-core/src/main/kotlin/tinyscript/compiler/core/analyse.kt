package tinyscript.compiler.core

import tinyscript.compiler.core.parser.TinyScriptParser
import tinyscript.compiler.scope.*
import tinyscript.compiler.util.SafeLazy

class AnalysisException(message: String) : Throwable(message)

class StatementCollection(
	val scope: Scope,
	val orderedStatements: List<Statement>
) {
	fun impureStatementsSeq(): Sequence<ImperativeStatement> = orderedStatements
		.asSequence()
		.mapNotNull { it as? ImperativeStatement }
		.filter { it.expression.get().isImpure }
}

fun TinyScriptParser.StatementListContext.analyse(parentScope: Scope?): StatementCollection =
	statement().analyse(parentScope)

fun Iterable<TinyScriptParser.StatementContext>.analyse(parentScope: Scope?): StatementCollection {
	val entityCollection = MutableEntityCollection()
	val scope = Scope(parentScope, entityCollection)
	val orderedStatements: MutableList<Statement> = ArrayList()
	val statements: List<Statement> = map { statementCtx ->
		when (statementCtx) {
			is TinyScriptParser.ImperativeStatementContext -> {
				val name: String? = statementCtx.Name()?.text
				val imperativeStatement = ImperativeStatement(name, SafeLazy { isRoot ->
					val expression = statementCtx.expression().analyse(scope)

					if (!isRoot && expression.isImpure)
						throw AnalysisException("can not forward reference an impure declaration")

					expression
				})
				if (name != null) {
					entityCollection.valueEntities.add(ValueEntity(
						NameSignature(null, name, false, null),
						{ imperativeStatement.expression.get().type }
					))
				}
				imperativeStatement
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
				functionDeclaration
			}
			else -> throw RuntimeException("unknown statement class")
		}
	}

	statements.forEach { statement ->
		if (statement is ImperativeStatement) {
			statement.expression.get(true)
		}
	}

	return StatementCollection(scope, orderedStatements)
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
	is TinyScriptParser.NameReferenceExpressionContext -> {
		val name: String = Name().text
		val isImpure: Boolean = Impure() != null
		val argumentsObjectExpression: ObjectExpression? = `object`()?.analyse(scope)
		val valueEntity: ValueEntity = scope.findValueEntity(NameSignature(
			null,
			name,
			isImpure,
			argumentsObjectExpression?.let { { it.type } }
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
