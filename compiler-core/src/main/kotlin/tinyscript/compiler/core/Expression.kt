package tinyscript.compiler.core

sealed class Expression {
	abstract val type: Type

	abstract val isImpure: Boolean
}
