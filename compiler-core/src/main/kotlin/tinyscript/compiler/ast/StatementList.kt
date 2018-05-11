package tinyscript.compiler.ast

import tinyscript.compiler.parser.TinyScriptParser
import tinyscript.compiler.scope.*
import tinyscript.compiler.util.SafeLazy

class StatementList(
	val scope: Scope,
	val orderedStatements: List<Statement>,
	val hasImpureImperativeStatement: Boolean
)

class ImpureForwardReferenceException(val fieldName: String) : RuntimeException(
	"can not forward reference an impure imperative declaration ($fieldName)"
)

class PureFunctionWithImpureExpressionException(val signatureExpression: SignatureExpression) : RuntimeException(
	"function signature must be impure if its expression is impure"
)

class DisallowedImpureStatementException : RuntimeException(
	"impure imperative statements not allowed here"
)

fun TinyScriptParser.StatementListContext.analyse(parentScope: Scope?): StatementList =
	statement().analyse(parentScope)

fun Iterable<TinyScriptParser.StatementContext>.analyse(parentScope: Scope?): StatementList {
	val scope = LazyScope(parentScope)
	val orderedStatements: MutableList<Statement> = ArrayList()
	val lazyStatementList: MutableList<SafeLazy<Statement>> = ArrayList()
	var hasImpureImperativeStatement: Boolean = false

	// first make sure all the types are in the scope
	forEach { statementCtx ->
		when (statementCtx) {
			is TinyScriptParser.TypeAliasDefinitionContext -> {
				val name = statementCtx.Name().text

				val lazyTypeAliasDefinition = SafeLazy {
					val typeExpression = statementCtx.typeExpression().analyse(scope)
					TypeAliasDefinition(name, typeExpression)
						.also { orderedStatements.add(it) }
				}
				scope.lazyTypeMap[name] = { lazyTypeAliasDefinition.get().typeExpression.type }
				lazyStatementList.add(lazyTypeAliasDefinition)
			}
			is TinyScriptParser.NativeTypeDeclarationContext -> {
				val name = statementCtx.Name().text

				val nativeTypeDeclaration = NativeTypeDeclaration(name, AtomicType())
					.also { orderedStatements.add(it) }
				scope.lazyTypeMap[name] = { nativeTypeDeclaration.atomicType }
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
							throw ImpureForwardReferenceException(name!!)

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
			is TinyScriptParser.FunctionDefinitionContext -> {
				val signatureExpression = statementCtx.signature().analyse(scope)
				val signature = signatureExpression.signature

				val lazyFunctionDefinition = SafeLazy {
					val thisScope = if (signature is NameSignature && signature.lhsType != null)
						ThisScope(scope, signature.lhsType) else scope

					val functionScope: Scope = when (signature) {
						is NameSignature -> {
							if (signature.paramsObjectType != null)
								FunctionParamsScope(thisScope, signature.paramsObjectType)
							else thisScope
						}
						is OperatorSignature -> OperatorFunctionScope(
							thisScope,
							signature.lhsType,
							signature.rhsType
						)
					}

					val expression = statementCtx.expression().analyse(functionScope)
					if (!signatureExpression.signature.isImpure && expression.isImpure)
						throw PureFunctionWithImpureExpressionException(signatureExpression)

					FunctionDefinition(signatureExpression, expression)
						.also { orderedStatements.add(it) }
				}
				scope.lazyFunctionMap.add(signatureExpression.signature, {
					lazyFunctionDefinition.get().expression.type
				})
				lazyStatementList.add(lazyFunctionDefinition)
			}
			is TinyScriptParser.NativeDeclarationContext -> {
				val signatureExpression = statementCtx.signature().analyse(scope)

				val lazyNativeDeclaration = SafeLazy {
					val typeExpression = statementCtx.typeExpression().analyse(scope)

					NativeDeclaration(signatureExpression, typeExpression)
						.also { orderedStatements.add(it) }
				}
				scope.lazyFunctionMap.add(signatureExpression.signature, {
					lazyNativeDeclaration.get().typeExpression.type
				})
				lazyStatementList.add(lazyNativeDeclaration)
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
			throw DisallowedImpureStatementException()
	}
