package tinyscript.compiler.core

import tinyscript.compiler.core.parser.TinyScriptParser
import tinyscript.compiler.util.Deferred
import java.util.*
import kotlin.collections.ArrayList

class AnalysisException(message: String) : Throwable(message)

class DeclarationCollection(
	val scope: Scope,
	val orderedDeclarations: List<Declaration>
)

fun Iterable<TinyScriptParser.DeclarationContext>.analyse(parentScope: Scope?): DeclarationCollection {
	val entityCollection = MutableEntityCollection()
	val scope = Scope(parentScope, entityCollection)
	val orderedDeclarations: LinkedList<Declaration> = LinkedList()

	val deferreds: MutableList<Deferred<*>> = ArrayList()
	forEach { declarationCtx ->
		when (declarationCtx) {
			is TinyScriptParser.NameDeclarationContext -> {
				val name: String = declarationCtx.Name().text
				val isImpure: Boolean = declarationCtx.Impure() != null
				val deferredType: Deferred<Type> = Deferred {
					val initializer = declarationCtx.initializer().analyse(scope)
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
					val initializer = declarationCtx.initializer().analyse(scope)
					orderedDeclarations.add(FunctionDeclaration(
						name,
						deferredParamsObjectType.get(),
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
			is TinyScriptParser.NonDeclarationContext -> {
				val deferredExpression = Deferred {
					declarationCtx.expression().analyse(scope)
						.also { expression ->
							orderedDeclarations.add(NonDeclaration(
								expression
							))
						}
				}
				deferreds.add(deferredExpression)
			}
			else -> TODO()
		}
	}

	// finalize all deferreds that are not finalized yet
	deferreds.forEach { it.get() }

	return DeclarationCollection(scope, orderedDeclarations)
}

fun TinyScriptParser.InitializerContext.analyse(scope: Scope): Initializer {
	val explicitType: Type? = typeExpression()?.analyse(scope)
	val expression = expression().analyse(scope)
	// TODO check if explicit type accepts expression type
	return Initializer(explicitType, expression)
}

fun TinyScriptParser.ExpressionContext.analyse(scope: Scope): Expression = when (this) {
	is TinyScriptParser.BlockExpressionContext ->
		block().analyse(scope)
	is TinyScriptParser.IntegerLiteralExpressionContext ->
		IntExpression(text.toInt())
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
	else -> TODO()
}

fun TinyScriptParser.BlockContext.analyse(scope: Scope): BlockExpression {
	val declarationCollection = declarations().declaration().analyse(scope)
	return BlockExpression(declarationCollection, expression().analyse(declarationCollection.scope))
}

fun TinyScriptParser.ObjectContext.analyse(scope: Scope): ObjectExpression {
	val declarationCollection = declarations().declaration().analyse(scope)
	return ObjectExpression(declarationCollection)
}

fun TinyScriptParser.TypeExpressionContext.analyse(scope: Scope): Type = when (this) {
	is TinyScriptParser.ParenTypeExpressionContext -> typeExpression().analyse(scope)
	is TinyScriptParser.ObjectTypeExpressionContext -> objectType().analyse(scope)
	else -> TODO()
}

fun TinyScriptParser.ObjectTypeContext.analyse(scope: Scope): ObjectType = TODO()
