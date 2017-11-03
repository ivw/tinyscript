grammar TinyScript;

@header {package tinyscript.compiler.core.parser;}

// start
file: NL* declarations? NL* EOF;

declarations: declaration ((',' | NL+) declaration)*;

declaration
	:	Name Impure? initializer								# SymbolDeclaration
	|	Name object Impure? initializer							# MethodDeclaration
	|	(lhs=type)? Operator Impure? rhs=type initializer		# OperatorDeclaration
	|	'class' Name object										# ClassDeclaration
	|	expression												# NonDeclaration
	|	'&' expression											# InheritDeclaration
	;

initializer
	:	':' type												# AbstractInitializer
	|	(':' type)? '=' NL* expression							# ConcreteInitializer
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
	|	Name Impure?											# ReferenceExpression
	|	expression NL* '.' Name Impure?							# DotReferenceExpression
	|	object													# ObjectExpression
	|	expression object Impure?								# FunctionCallExpression
	|	Operator Impure? expression								# PrefixOperatorCallExpression
	|	expression NL* Operator Impure? NL* expression			# InfixOperatorCallExpression
	|	'if' NL* (block expression NL*)+ 'else' expression		# ConditionalExpression
	|	object? Impure? '->' NL* expression						# FunctionExpression
	;

block: '(' NL* (declarations (',' | NL+))? expression NL* ')';

object: '[' NL* declarations? NL* ']';

type
	:	'(' type ')'											# ParenType
	|	objectType? Impure? '->' type							# FunctionType
	|	'?'														# NullType
	|	type '?'												# NullableType
	|	objectType												# ObjectTypeType
	|	Name													# TypeReference
	;

objectType: '[' NL* (objectTypeField ((',' | NL+) objectTypeField)* NL*)? ']';

objectTypeField
	:	Name ':' type											# SymbolObjectTypeField
	|	'&' Name												# InheritDeclarationObjectTypeField
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
