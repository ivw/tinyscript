package tinyscript.compiler.core

import tinyscript.compiler.core.parser.TinyScriptParser

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

}

fun TinyScriptParser.DeclarationContext.analyse(): Declaration = when (this) {
	is TinyScriptParser.AbstractDeclarationContext ->
		AbstractDeclaration(signature().analyse(), type())
	is TinyScriptParser.ConcreteDeclarationContext ->
		ConcreteDeclaration(signature().analyse(), type(), expression())
	else -> TODO()
}

fun TinyScriptParser.SignatureContext.analyse(): Signature = when (this) {
	is TinyScriptParser.SymbolSignatureContext ->
		SymbolSignature(Name().text, Impure() != null)
	is TinyScriptParser.FunctionSignatureContext ->
		FunctionSignature(Name().text, `object`(), Impure() != null)
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
