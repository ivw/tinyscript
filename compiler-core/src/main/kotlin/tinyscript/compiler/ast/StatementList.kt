package tinyscript.compiler.ast

import tinyscript.compiler.parser.TinyScriptParser
import tinyscript.compiler.scope.DeclarationScope
import tinyscript.compiler.scope.FunctionScope
import tinyscript.compiler.scope.NameSignature
import tinyscript.compiler.scope.Scope
import tinyscript.compiler.util.SafeLazy

class StatementList(
	val scope: Scope,
	val orderedStatements: List<Statement>,
	val hasImpureImperativeStatement: Boolean
)

fun TinyScriptParser.StatementListContext.analyse(parentScope: Scope?): StatementList =
	statement().analyse(parentScope)

fun Iterable<TinyScriptParser.StatementContext>.analyse(parentScope: Scope?): StatementList {
	val scope = DeclarationScope(parentScope)
	val orderedStatements: MutableList<Statement> = ArrayList()
	val lazyStatementList: MutableList<SafeLazy<Statement>> = ArrayList()
	var hasImpureImperativeStatement: Boolean = false

	// first make sure all the types are in the scope
	forEach { statementCtx ->
		when (statementCtx) {
			is TinyScriptParser.TypeAliasDeclarationContext -> {
				val name = statementCtx.Name().text

				val lazyTypeAliasDeclaration = SafeLazy {
					val typeExpression = statementCtx.typeExpression().analyse(scope)
					TypeAliasDeclaration(name, typeExpression)
						.also { orderedStatements.add(it) }
				}
				scope.lazyTypeMap[name] = { lazyTypeAliasDeclaration.get().typeExpression.type }
				lazyStatementList.add(lazyTypeAliasDeclaration)
			}
		}
	}

	// now add all the values to scope (signatures might need the types)
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
					scope.lazyFieldMap[name] = { lazyImperativeStatement.get().expression.type }
				}
				lazyStatementList.add(lazyImperativeStatement)
			}
			is TinyScriptParser.FunctionDeclarationContext -> {
				val signatureExpression = statementCtx.signature().analyse(scope)
				val signature = signatureExpression.signature

				val lazyFunctionDeclaration = SafeLazy {
					val functionScope: Scope = if (signature is NameSignature && signature.paramsObjectType != null) {
						FunctionScope(scope, signature.paramsObjectType)
					} else scope
					val expression = statementCtx.expression().analyse(functionScope)
					if (expression.isImpure && !signatureExpression.signature.isImpure)
						throw AnalysisException("function signature must be impure if its expression is impure")

					FunctionDeclaration(signatureExpression, expression)
						.also { orderedStatements.add(it) }
				}
				scope.lazyFunctionMap.add(signatureExpression.signature, {
					lazyFunctionDeclaration.get().expression.type
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

fun Iterable<TinyScriptParser.StatementContext>.analysePure(parentScope: Scope?): StatementList =
	analyse(parentScope).also {
		if (it.hasImpureImperativeStatement)
			throw AnalysisException("file scope can not have impure imperative statements")
	}
