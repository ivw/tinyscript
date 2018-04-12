package tinyscript.compiler.core

import tinyscript.compiler.core.parser.TinyScriptParser
import tinyscript.compiler.scope.*
import tinyscript.compiler.util.SafeLazy

class AnalysisException(message: String) : Throwable(message)

class StatementCollection(
	val scope: Scope,
	val orderedStatements: List<Statement>,
	val hasImpureImperativeStatement: Boolean
)

fun TinyScriptParser.StatementListContext.analyse(parentScope: Scope?): StatementCollection =
	statement().analyse(parentScope)

fun Iterable<TinyScriptParser.StatementContext>.analyse(parentScope: Scope?): StatementCollection {
	val entityCollection = MutableEntityCollection()
	val scope = Scope(parentScope, entityCollection)
	val orderedStatements: MutableList<Statement> = ArrayList()
	val lazyStatementList: MutableList<SafeLazy<Statement>> = ArrayList()
	var hasImpureImperativeStatement: Boolean = false

	// first make sure all the type entities are in the scope
	forEach { statementCtx ->
		when (statementCtx) {
			is TinyScriptParser.TypeAliasDeclarationContext -> {
				val name = statementCtx.Name().text

				val lazyTypeAliasDeclaration = SafeLazy {
					val typeExpression = statementCtx.typeExpression().analyse(scope)
					TypeAliasDeclaration(name, typeExpression)
						.also { orderedStatements.add(it) }
				}
				entityCollection.typeEntities.add(TypeEntity(
					name,
					{ lazyTypeAliasDeclaration.get().typeExpression.type }
				))
				lazyStatementList.add(lazyTypeAliasDeclaration)
			}
		}
	}

	// now add all the value entities to scope (some entity signatures need the type entities)
	forEach { statementCtx ->
		when (statementCtx) {
			is TinyScriptParser.ImperativeStatementContext -> {
				val name: String? = statementCtx.Name()?.text

				val lazyImperativeStatement = SafeLazy { isRoot ->
					val expression = statementCtx.expression().analyse(scope)
					if (expression.isImpure) {
						if (!isRoot)
							throw AnalysisException("can not forward reference an impure imperative declaration")

						hasImpureImperativeStatement = true
					}

					ImperativeStatement(name, expression)
						.also { orderedStatements.add(it) }
				}

				if (name != null) {
					entityCollection.valueEntities.add(ValueEntity(
						NameSignature(null, name, false, null),
						{ lazyImperativeStatement.get().expression.type }
					))
				}
				lazyStatementList.add(lazyImperativeStatement)
			}
			is TinyScriptParser.FunctionDeclarationContext -> {
				val signatureExpression = statementCtx.signature().analyse(scope)

				val lazyFunctionDeclaration = SafeLazy {
					val expression = statementCtx.expression().analyse(scope)
					if (expression.isImpure && !signatureExpression.signature.isImpure)
						throw AnalysisException("function signature must be impure if its expression is impure")

					FunctionDeclaration(signatureExpression, expression)
						.also { orderedStatements.add(it) }
				}
				entityCollection.valueEntities.add(ValueEntity(
					signatureExpression.signature,
					{ lazyFunctionDeclaration.get().expression.type }
				))
				lazyStatementList.add(lazyFunctionDeclaration)
			}
			else -> throw RuntimeException("unknown statement class")
		}
	}

	// analyse all statements now that everything is in the scope
	lazyStatementList.forEach { it.get(true) }
	orderedStatements.forEach { it.finalize() }
	// example:
	/*
		type Node = ([
			parent: Option<Node>

			children: List<Node>
		])
	 */
	// after `get(true)` we know the type of Node is an object with an Option<?> field and a List<?> field.
	// `finalize()` will make sure all the contained types are analysed.

	return StatementCollection(scope, orderedStatements, hasImpureImperativeStatement)
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
	val statementCollection = statementList()?.analyse(scope)
	return BlockExpression(
		statementCollection,
		expression().analyse(statementCollection?.scope ?: scope)
	)
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
