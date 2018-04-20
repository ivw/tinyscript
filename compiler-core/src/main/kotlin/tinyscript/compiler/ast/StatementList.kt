package tinyscript.compiler.ast

import tinyscript.compiler.ast.parser.TinyScriptParser
import tinyscript.compiler.scope.*
import tinyscript.compiler.util.SafeLazy

class StatementList(
	val scope: Scope,
	val orderedStatements: List<Statement>,
	val hasImpureImperativeStatement: Boolean
)

fun TinyScriptParser.StatementListContext.analyse(parentScope: Scope?): StatementList =
	statement().analyse(parentScope)

fun Iterable<TinyScriptParser.StatementContext>.analyse(parentScope: Scope?): StatementList {
	val entityCollection = MutableEntityCollection()
	val scope = DeclarationScope(parentScope, entityCollection)
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
				entityCollection.typeEntities.add(object : TypeEntity(name) {
					override val type: Type get() = lazyTypeAliasDeclaration.get().typeExpression.type
				})
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
					entityCollection.valueEntities.add(object : ValueEntity(
						NameSignature(null, name, false, null)
					) {
						override val type: Type get() = lazyImperativeStatement.get().expression.type
					})
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
				entityCollection.valueEntities.add(object : ValueEntity(
					signatureExpression.signature
				) {
					override val type: Type get() = lazyFunctionDeclaration.get().expression.type
				})
				lazyStatementList.add(lazyFunctionDeclaration)
			}
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

	return StatementList(scope, orderedStatements, hasImpureImperativeStatement)
}
