package tinyscript.compiler.core

import tinyscript.compiler.core.parser.TinyScriptParser

val globalScope = Scope(null)

class Scope(
	val parentScope: Scope?,
	val declarations: List<Declaration> = ArrayList()
) {
	val depth: Int = if (parentScope == null) 0 else parentScope.depth + 1

	fun resolve(predicate: (Declaration) -> Boolean): Declaration? =
		declarations.lastOrNull(predicate) ?: parentScope?.resolve(predicate)
}

fun TinyScriptParser.FileContext.analyse() =
	declarations().analyseDeclarative(globalScope)

fun TinyScriptParser.DeclarationsContext.analyseDeclarative(parentScope: Scope): Scope =
	Scope(parentScope, declaration().map { it.analyse() }).apply {
		// now that the scope is filled completely, we can finish the declaration analyses
		declarations.forEach { it.finishAnalysis(this) }
	}

// in an impure imperative scope (like a blockScope), the order matters, and there can be no forward references.
fun TinyScriptParser.DeclarationsContext.analyseImperative(parentScope: Scope): Scope {
	val mutableDeclarations: MutableList<Declaration> = ArrayList()
	return Scope(parentScope, mutableDeclarations).apply {
		declaration().forEach { declarationCtx ->
			val declaration = declarationCtx.analyse()
			mutableDeclarations.add(declaration)
			declaration.finishAnalysis(this)
		}
	}
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
