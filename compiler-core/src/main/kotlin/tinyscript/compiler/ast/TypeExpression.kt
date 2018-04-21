package tinyscript.compiler.ast

import tinyscript.compiler.parser.TinyScriptParser
import tinyscript.compiler.scope.*

sealed class TypeExpression {
	abstract val type: Type
}

class ParenTypeExpression(val typeExpression: TypeExpression) : TypeExpression() {
	override val type get() = typeExpression.type
}

class TypeReferenceExpression(
	val name: String,
	val typeResult: TypeResult
) : TypeExpression() {
	override val type: Type = typeResult.type
}

class FunctionTypeExpression(
	val isImpure: Boolean,
	val objectTypeExpression: ObjectTypeExpression?,
	val returnTypeExpression: TypeExpression
) : TypeExpression() {
	override val type = FunctionType(
		isImpure,
		objectTypeExpression?.type,
		returnTypeExpression.type
	)
}

class ObjectTypeExpression(val objectTypeStatements: List<ObjectTypeStatement>) : TypeExpression() {
	override val type = ObjectType(mutableMapOf<String, Type>().also { mutableFieldMap ->
		objectTypeStatements.forEach { objectTypeStatement ->
			when (objectTypeStatement) {
				is ObjectTypeFieldDeclaration -> {
					mutableFieldMap[objectTypeStatement.name] = objectTypeStatement.typeExpression.type
				}
				is ObjectTypeInheritStatement -> {
					TODO()
				}
			}
		}
	})
}

fun TinyScriptParser.TypeExpressionContext.analyse(scope: Scope): TypeExpression = when (this) {
	is TinyScriptParser.ParenTypeExpressionContext -> ParenTypeExpression(typeExpression().analyse(scope))
	is TinyScriptParser.FunctionTypeExpressionContext -> FunctionTypeExpression(
		Impure() != null,
		objectType()?.analyse(scope),
		typeExpression().analyse(scope)
	)
	is TinyScriptParser.ObjectTypeExpressionContext -> objectType().analyse(scope)
	is TinyScriptParser.TypeReferenceExpressionContext -> {
		val name: String = Name().text
		val typeResult: TypeResult = scope.findType(name)
			?: throw AnalysisException("unresolved type reference '$name'")
		TypeReferenceExpression(name, typeResult)
	}
	else -> TODO()
}

fun TinyScriptParser.ObjectTypeContext.analyse(scope: Scope): ObjectTypeExpression =
	ObjectTypeExpression(objectTypeStatement().map { it.analyse(scope) })

