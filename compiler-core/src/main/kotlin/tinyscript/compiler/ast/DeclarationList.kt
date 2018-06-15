package tinyscript.compiler.ast

import tinyscript.compiler.parser.TinyScriptParser
import tinyscript.compiler.scope.*
import tinyscript.compiler.util.SafeLazy

class DeclarationList(
	val scope: Scope,
	val orderedDeclarations: List<Declaration>
)

class TypeMutableException(val name: String) : RuntimeException(
	"type must have `!` iff it is mutable"
)

class FunctionImpureException : RuntimeException(
	"function must have `!` iff it is impure (has mutable input or mutable output)"
)

fun Iterable<TinyScriptParser.DeclarationContext>.analyse(parentScope: Scope?): DeclarationList {
	val scope = LazyScope(parentScope)
	val orderedDeclarations: MutableList<Declaration> = ArrayList()
	val lazyDeclarationList: MutableList<SafeLazy<Declaration>> = ArrayList()

	// first make sure all the types are in the scope
	forEach { declarationCtx ->
		when (declarationCtx) {
			is TinyScriptParser.TypeAliasDefinitionContext -> {
				val name = declarationCtx.TypeName().text
				val isMutable = declarationCtx.Impure() != null

				val lazyTypeAliasDefinition = SafeLazy {
					val typeExpression = declarationCtx.typeExpression().analyse(scope)
					if (typeExpression.type.isMutable != isMutable)
						throw TypeMutableException(name)

					TypeAliasDefinition(name, typeExpression)
						.also { orderedDeclarations.add(it) }
				}
				scope.addType(name, isMutable, { lazyTypeAliasDefinition.get().typeExpression.type })
				lazyDeclarationList.add(lazyTypeAliasDefinition)
			}
			is TinyScriptParser.NativeTypeDeclarationContext -> {
				val name = declarationCtx.TypeName().text
				val isMutable = declarationCtx.Impure() != null

				val nativeTypeDeclaration = NativeTypeDeclaration(name, AtomicType(isMutable))
					.also { orderedDeclarations.add(it) }
				scope.addType(name, isMutable, { nativeTypeDeclaration.atomicType })
			}
		}
	}

	// now add all the values to scope (signatures might need the types)
	forEach { declarationCtx ->
		when (declarationCtx) {
			is TinyScriptParser.FunctionDefinitionContext -> {
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
					if (signature.isImpure != signature.hasMutableInput || expression.type.isMutable)
						throw FunctionImpureException()

					FunctionDefinition(signatureExpression, expression)
						.also { orderedDeclarations.add(it) }
				}
				scope.addFunction(signature, {
					lazyFunctionDefinition.get().expression.type
				})
				lazyDeclarationList.add(lazyFunctionDefinition)
			}
			is TinyScriptParser.NativeFunctionDeclarationContext -> {
				val signatureExpression = declarationCtx.signature().analyse(scope)

				val lazyNativeFunctionDeclaration = SafeLazy {
					val typeExpression = declarationCtx.typeExpression().analyse(scope)

					NativeFunctionDeclaration(signatureExpression, typeExpression)
						.also { orderedDeclarations.add(it) }
				}
				scope.addFunction(signatureExpression.signature, {
					lazyNativeFunctionDeclaration.get().typeExpression.type
				})
				lazyDeclarationList.add(lazyNativeFunctionDeclaration)
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
