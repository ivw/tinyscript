package tinyscript.compiler.core

import tinyscript.compiler.core.parser.TinyScriptParser
import tinyscript.compiler.util.Deferred
import java.util.*
import kotlin.collections.ArrayList

class Scope(
	val parentScope: Scope?,
	val entities: List<Entity>
) {
	val depth: Int = if (parentScope == null) 0 else parentScope.depth + 1

	fun resolve(predicate: (Entity) -> Boolean): Entity? =
		entities.lastOrNull(predicate) ?: parentScope?.resolve(predicate)
}

class DeclarationCollection(
	val scope: Scope,
	val orderedDeclarations: List<Declaration>
)

fun Iterable<TinyScriptParser.DeclarationContext>.analyse(parentScope: Scope?): DeclarationCollection {
	val entities: MutableList<Entity> = ArrayList()
	val scope = Scope(parentScope, entities)
	val orderedDeclarations: LinkedList<Declaration> = LinkedList()

	val deferreds: MutableList<Deferred<*>> = ArrayList()
	forEach { declarationCtx ->
		when (declarationCtx) {
			is TinyScriptParser.TypeDeclarationContext -> {
				val deferredType = Deferred {
					val typeDeclaration = TypeDeclaration(
						declarationCtx.Name().text,
						declarationCtx.type().analyse(scope)
					)
					orderedDeclarations.add(typeDeclaration)
					typeDeclaration.type
				}
				entities.add(TypeEntity(
					declarationCtx.Name().text,
					deferredType
				))
				deferreds.add(deferredType)
			}
			else -> TODO()
		}
	}

	// finalize all deferreds that are not finalized yet
	deferreds.forEach { it.get() }

	return DeclarationCollection(scope, orderedDeclarations)
}

fun TinyScriptParser.SignatureContext.analyse(scope: Scope): Signature = when (this) {
	is TinyScriptParser.SymbolSignatureContext ->
		SymbolSignature(Name().text, Impure() != null)
	is TinyScriptParser.FunctionSignatureContext ->
		FunctionSignature(Name().text, objectType().analyse(scope), Impure() != null)
	else -> TODO()
}

fun TinyScriptParser.ExpressionContext.analyse(scope: Scope): Expression = when (this) {
	is TinyScriptParser.BlockExpressionContext ->
		block().analyse(scope)
	is TinyScriptParser.IntegerLiteralExpressionContext ->
		IntExpression(text.toInt())
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

fun TinyScriptParser.TypeContext.analyse(scope: Scope): Type = when (this) {
	is TinyScriptParser.ParenTypeContext -> type().analyse(scope)
	else -> TODO()
}

fun TinyScriptParser.ObjectTypeContext.analyse(scope: Scope): ObjectType = TODO()
