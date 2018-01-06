package tinyscript.compiler.core

import tinyscript.compiler.core.parser.TinyScriptParser

abstract class Declaration {
	var isFinishing: Boolean = false

	abstract fun finishAnalysis(scope: Scope): Unit
}

class AbstractDeclaration(
	val signature: Signature,
	private val typeCtx: TinyScriptParser.TypeContext
) : Declaration() {
	override fun finishAnalysis(scope: Scope) {

	}
}

class ConcreteDeclaration(
	private val signature: Signature,
	private val typeCtx: TinyScriptParser.TypeContext?,
	private val expressionCtx: TinyScriptParser.ExpressionContext
) : Declaration() {
	override fun finishAnalysis(scope: Scope) {

	}
}

abstract class Signature

class SymbolSignature(
	val name: String,
	val isImpure: Boolean
) : Signature()

class FunctionSignature(
	val name: String,
	val objectCtx: TinyScriptParser.ObjectContext,
	val isImpure: Boolean
) : Signature()
