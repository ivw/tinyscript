package tinyscript.compiler.core

import tinyscript.compiler.core.parser.TinyScriptParser
import tinyscript.compiler.scope.*

sealed class TypeExpression {
	abstract val type: Type
}

class ParenTypeExpression(val typeExpression: TypeExpression) : TypeExpression() {
	override val type get() = typeExpression.type
}

class TypeReferenceExpression(
	val name: String,
	override val type: Type
) : TypeExpression()

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
		val typeEntity: TypeEntity = scope.findTypeEntity(name)
			?: throw AnalysisException("unresolved type reference '$name'")
		TypeReferenceExpression(name, typeEntity.getType())
	}
	else -> TODO()
}

fun TinyScriptParser.ObjectTypeContext.analyse(parentScope: Scope): ObjectTypeExpression = TODO()

