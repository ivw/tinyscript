grammar TinyScript;

@header {package tinyscript.compiler.core.parser;}

// start
file: NL* (declaration ((',' | NL+) declaration)* NL*)? EOF;

declaration
	:	signature ':' type										# AbstractDeclaration
	|	signature (':' type)? '=' NL* expression				# ConcreteDeclaration
	|	expression												# ImplicitDeclaration
	|	'&' expression											# InheritDeclaration
	;

signature
	:	Mut? Name												# Symbol
	|	Operator type											# PrefixOperator
	|	type Operator type										# InfixOperator
	|	(type '.')? Name object									# Method
	;

expression
	:	block													# BlockExpression
	|	IntegerLiteral											# IntegerLiteralExpression
	|	FloatLiteral											# FloatLiteralExpression
	|	StringLiteral											# StringLiteralExpression
	|	BooleanLiteral											# BooleanLiteralExpression
	|	('<' type '>')? '?'										# NullExpression
	|	'this'													# ThisExpression
	|	'super'													# SuperExpression
	|	Name													# ReferenceExpression
	|	expression NL* '.' Name									# DotReferenceExpression
	|	Mut? object												# ObjectExpression
	|	expression object										# FunctionCallExpression
	|	Operator expression										# PrefixOperatorCallExpression
	|	expression NL* Operator NL* expression					# InfixOperatorCallExpression
	|	Mut? 'class' object										# ClassExpression
	|	'if' NL* (block expression NL*)+ 'else' expression		# ConditionalExpression
	|	Name '<-' expression									# ReassignmentExpression
	|	expression NL* '.' Name '<-' expression					# DotReassignmentExpression
	|	object? Mut? '->' NL* expression						# FunctionExpression
	;

block: '(' NL* (declaration (';' | NL+))* expression NL* ')';

object: '[' NL* (declaration ((',' | NL+) declaration)* NL*)? ']';

type
	:	'(' type ')'											# ParenType
	|	objectType? Mut? '->' type								# FunctionType
	|	'?'														# NullType
	|	type '?'												# NullableType
	|	Mut? objectType											# ObjectTypeType
	|	Name													# TypeReference
	|	type '|' type											# IntersectObjectType
	;

objectType: '[' NL* (objectTypeField ((',' | NL+) objectTypeField)* NL*)? ']';

objectTypeField
	:	Mut? Name ':' type										# SymbolObjectTypeField
	|	'&' Name												# InheritDeclarationObjectTypeField
	;

// LEXER TOKENS

Private: 'private';
Override: 'override';
Mut: '#';

IntegerLiteral: [0-9]+;

FloatLiteral: [0-9]+ '.' [0-9]+;

BooleanLiteral: 'true' | 'false';

StringLiteral: '"' StringCharacter* '"';

fragment
StringCharacter: ~["\\];

Operator: '+' | '-' | '*' | '/' | '^' | '%' | '!' | '==' | '!=';

Name: [a-zA-Z$_] [a-zA-Z$_0-9]*;

NL: [\r\n]+;

WS: [ \t\u000C]+ -> skip;

Comment: '/*' .*? '*/' -> channel(HIDDEN);

LineComment: '//' ~[\r\n]* -> channel(HIDDEN);
