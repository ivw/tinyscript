package tinyscript.compiler.ast

import tinyscript.compiler.parser.TinyScriptParser
import tinyscript.compiler.scope.*

sealed class TypeExpression {
	abstract val type: Type
}

class AnyTypeExpression: TypeExpression() {
	override val type = AnyType
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

class TypeAliasNotFoundException(val name: String) : RuntimeException(
	"unresolved type reference '$name'"
)

fun TinyScriptParser.TypeExpressionContext.analyse(scope: Scope): TypeExpression = when (this) {
	is TinyScriptParser.ParenTypeExpressionContext ->
		typeExpression()?.analyse(scope) ?: AnyTypeExpression()
	is TinyScriptParser.FunctionTypeExpressionContext -> FunctionTypeExpression(
		Impure() != null,
		objectType()?.analyse(scope),
		typeExpression().analyse(scope)
	)
	is TinyScriptParser.ObjectTypeExpressionContext -> objectType().analyse(scope)
	is TinyScriptParser.TypeReferenceExpressionContext -> {
		val name: String = Name().text
		val typeResult: TypeResult = scope.findType(name)
			?: throw TypeAliasNotFoundException(name)
		TypeReferenceExpression(name, typeResult)
	}
	else -> TODO()
}

fun TinyScriptParser.ObjectTypeContext.analyse(scope: Scope): ObjectTypeExpression =
	ObjectTypeExpression(objectTypeStatement().map { it.analyse(scope) })

