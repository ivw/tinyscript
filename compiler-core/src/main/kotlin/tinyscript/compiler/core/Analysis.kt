package tinyscript.compiler.core

import tinyscript.compiler.core.parser.TinyScriptParser
import tinyscript.compiler.util.Deferred

class Scope(
	val parentScope: Scope?,
	val entities: EntityCollection = EntityCollection()
) {
	val depth: Int = if (parentScope == null) 0 else parentScope.depth + 1

	fun resolve(predicate: (Entity) -> Boolean): Entity? =
		entities.lastOrNull(predicate) ?: parentScope?.resolve(predicate)
}

class DeclarationCollection(
	val scope: Scope,
	val orderedDeclarations: List<Declaration>
)

fun List<TinyScriptParser.DeclarationContext>.analyse(parentScope: Scope): DeclarationCollection {
	val scope = Scope(null, builtInEntities)
	val declarations: List<Declaration> = map { it.analyse(scope) }
	declarations.forEach { it.finalize() }

	TODO()
}

// Note: When this function is called, the scope is not filled yet. It is when `finalize` is called.
fun TinyScriptParser.DeclarationContext.analyse(scope: Scope): Declaration = when (this) {
	is TinyScriptParser.TypeDeclarationContext -> TypeDeclaration(
		Name().text,
		Deferred { type().analyse(scope) }
	).also {
		scope.entities.add(TypeEntity(it.name, it.deferredType))
	}
	else -> TODO()
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
