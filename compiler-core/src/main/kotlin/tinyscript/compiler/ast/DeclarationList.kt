package tinyscript.compiler.ast

import tinyscript.compiler.parser.TinyScriptParser
import tinyscript.compiler.scope.*
import tinyscript.compiler.util.SafeLazy

class DeclarationList(
	val scope: Scope,
	val orderedDeclarations: List<Declaration>
)

class PureFieldWithMutableTypeException : RuntimeException(
	"field signature must be impure if its type is mutable"
)

fun Iterable<TinyScriptParser.DeclarationContext>.analyse(parentScope: Scope?): DeclarationList {
	val scope = LazyScope(parentScope)
	val orderedDeclarations: MutableList<Declaration> = ArrayList()
	val lazyDeclarationList: MutableList<SafeLazy<Declaration>> = ArrayList()

	// first make sure all the types are in the scope
	forEach { declarationCtx ->
		when (declarationCtx) {
			is TinyScriptParser.TypeAliasDefinitionContext -> {
				val name = declarationCtx.Name().text

				val lazyTypeAliasDefinition = SafeLazy {
					val typeExpression = declarationCtx.typeExpression().analyse(scope)
					TypeAliasDefinition(name, typeExpression)
						.also { orderedDeclarations.add(it) }
				}
				scope.lazyTypeMap[name] = { lazyTypeAliasDefinition.get().typeExpression.type }
				lazyDeclarationList.add(lazyTypeAliasDefinition)
			}
			is TinyScriptParser.NativeTypeDeclarationContext -> {
				val name = declarationCtx.Name().text

				val nativeTypeDeclaration = NativeTypeDeclaration(name, AtomicType(false /* TODO */))
					.also { orderedDeclarations.add(it) }
				scope.lazyTypeMap[name] = { nativeTypeDeclaration.atomicType }
			}
		}
	}

	// now add all the values to scope (signatures might need the types)
	forEach { declarationCtx ->
		when (declarationCtx) {
			is TinyScriptParser.ImperativeStatementContext -> {
				val name: String? = declarationCtx.Name()?.text

				val lazyImperativeStatement = SafeLazy { isRoot ->
					val expression = declarationCtx.expression().analyse(scope)
					if (expression.isImpure) {
						if (!isRoot)
							throw ImpureForwardReferenceException(name!!)

						hasImpureImperativeStatement = true
					}
					if (name != null) {
						val isImpure = declarationCtx.Impure() != null
						if (expression.type.hasMutableState && !isImpure)
							throw PureFieldWithMutableTypeException()
					}

					ImperativeStatement(name, expression)
						.also { orderedDeclarations.add(it) }
				}

				if (name != null) {
					scope.lazyFieldMap[name] = { lazyImperativeStatement.get().expression.type }
				}
				lazyDeclarationList.add(lazyImperativeStatement)
			}
			is TinyScriptParser.ValueDefinitionContext -> {
				val signatureExpression = declarationCtx.signature().analyse(scope)
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

					val expression = declarationCtx.expression().analyse(functionScope)

					ValueDefinition(signatureExpression, expression)
						.also { orderedDeclarations.add(it) }
				}
				scope.lazyFunctionMap.add(signatureExpression.signature, {
					lazyFunctionDefinition.get().expression.type
				})
				lazyDeclarationList.add(lazyFunctionDefinition)
			}
			is TinyScriptParser.NativeDeclarationContext -> {
				val signatureExpression = declarationCtx.signature().analyse(scope)

				val lazyNativeDeclaration = SafeLazy {
					val typeExpression = declarationCtx.typeExpression().analyse(scope)

					NativeDeclaration(signatureExpression, typeExpression)
						.also { orderedDeclarations.add(it) }
				}
				scope.lazyFunctionMap.add(signatureExpression.signature, {
					lazyNativeDeclaration.get().typeExpression.type
				})
				lazyDeclarationList.add(lazyNativeDeclaration)
			}
		}
	}

	// analyse all declarations now that everything is in the scope
	lazyDeclarationList.forEach { it.get(true) }
	orderedDeclarations.forEach { it.finalize() }
	// example:
	/*
		type Node = ([
			parent: Option<Node>

			children: List<Node>
		])
	 */
	// after `get(true)` we know the type of Node is an object with an Option<?> field and a List<?> field.
	// `finalize()` will make sure all the contained types are analysed.

	return DeclarationList(scope, orderedDeclarations)
}
