grammar TinyScript;

@header {package tinyscript.compiler.core.parser;}

// start
file: NL* declarations? NL* EOF;

declarations: declaration ((',' | NL+) declaration)*;

declaration
	:	Name Impure? initializer												# NameDeclaration
	|	Name objectType Impure? initializer										# FunctionDeclaration
	|	(lhs=typeExpression)? Operator Impure? rhs=typeExpression initializer	# OperatorDeclaration
	|	'type' Name '=' typeExpression											# TypeAliasDeclaration
	|	'enum' Name '{' NL* Name ((',' | NL+) Name)* NL* '}'					# EnumTypeDeclaration
	|	expression																# NonDeclaration
	|	'&' expression															# InheritDeclaration
	;

initializer: (':' typeExpression)? '=' NL* expression;

expression
	:	block																	# BlockExpression
	|	IntegerLiteral															# IntegerLiteralExpression
	|	FloatLiteral															# FloatLiteralExpression
	|	StringLiteral															# StringLiteralExpression
	|	BooleanLiteral															# BooleanLiteralExpression
	|	('<' typeExpression '>')? '?'											# NullExpression
	|	'this'																	# ThisExpression
	|	'super'																	# SuperExpression
	|	Name Impure?															# ReferenceExpression
	|	expression NL* '.' Name Impure?											# DotReferenceExpression
	|	object																	# ObjectExpression
	|	expression object Impure?												# FunctionCallExpression
	|	Operator Impure? expression												# PrefixOperatorCallExpression
	|	expression NL* Operator Impure? NL* expression							# InfixOperatorCallExpression
	|	'if' NL* (block expression NL*)+ 'else' expression						# ConditionalExpression
	|	expression 'if' NL* (block expression NL*)+ 'else' expression			# ExprConditionalExpression // not sure yet.
	|	expression 'then' NL* expression										# SingleConditionalExpression
	|	object? Impure? '->' NL* expression										# FunctionExpression
	;

block: '(' NL* (declarations (',' | NL+))? expression NL* ')';

object: '[' NL* declarations? NL* ']';

typeExpression
	:	'(' typeExpression ')'													# ParenTypeExpression
	|	objectType? Impure? '->' typeExpression									# FunctionTypeExpression
	|	'?'																		# NullTypeExpression
	|	typeExpression '?'														# NullableTypeExpression
	|	objectType																# ObjectTypeExpression
	|	Name																	# TypeReferenceExpression
	|	typeExpression block													# DependentTypeExpression
	;

objectType: '[' NL* (objectTypeField ((',' | NL+) objectTypeField)* NL*)? ']';

objectTypeField
	:	Name Impure? ':' typeExpression											# NameObjectTypeField
	|	Name objectType Impure? ':' typeExpression								# FunctionObjectTypeField
	|	(lhs=typeExpression)? Operator Impure? rhs=typeExpression ':' typeExpression	# OperatorObjectTypeField
	|	'&' Name																# InheritDeclarationObjectTypeField
	;

// LEXER TOKENS

Private: 'private';
Override: 'override';
Impure: '!';

IntegerLiteral: [0-9]+;

FloatLiteral: [0-9]+ '.' [0-9]+;

BooleanLiteral: 'true' | 'false';

StringLiteral: '"' StringCharacter* '"';

fragment
StringCharacter: ~["\\];

Operator: '+' | '-' | '*' | '/' | '^' | '%' | '==' | '!=';

Name: [a-zA-Z$_] [a-zA-Z$_0-9]*;

NL: [\r\n]+;

WS: [ \t\u000C]+ -> skip;

Comment: '/*' .*? '*/' -> channel(HIDDEN);

LineComment: '//' ~[\r\n]* -> channel(HIDDEN);
