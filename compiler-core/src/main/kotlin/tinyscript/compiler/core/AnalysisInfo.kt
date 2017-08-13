package tinyscript.compiler.core

open class AnalysisInfo(val scope: Scope)

open class ExpressionInfo(scope: Scope, val type: Type) : AnalysisInfo(scope)

open class OperatorInfo(scope: Scope, val operator: Operator) : AnalysisInfo(scope)

open class ReferenceExpressionInfo(
		scope: Scope,
		type: Type,
		val symbol: Symbol
) : ExpressionInfo(scope, type)

open class OperatorCallExpressionInfo(
		scope: Scope,
		type: Type,
		val operator: Operator
) : ExpressionInfo(scope, type)
