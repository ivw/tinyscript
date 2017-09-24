grammar TinyScript;

@header {package tinyscript.compiler.core.parser;}

// start
file: NL* (declaration ((';' | NL+) declaration)* NL*)? EOF;

declaration
	:	Mut? Name initializer									# SymbolDeclaration
	|	Name object initializer									# MethodDeclaration
	|	(lhs=type)? Operator rhs=type initializer				# OperatorDeclaration
	|	Mut? 'class' Name object								# ClassDeclaration
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
	|	Name													# ReferenceExpression
	|	expression NL* '.' Name									# DotReferenceExpression
	|	Mut? object												# ObjectExpression
	|	expression object										# FunctionCallExpression
	|	Operator expression										# PrefixOperatorCallExpression
	|	expression NL* Operator NL* expression					# InfixOperatorCallExpression
	|	'if' NL* (block expression NL*)+ 'else' expression		# ConditionalExpression
	|	Name '<-' expression									# ReassignmentExpression
	|	expression NL* '.' Name '<-' expression					# DotReassignmentExpression
	;

block: '(' NL* (declaration (';' | NL+))* expression NL* ')';

object: '[' NL* (declaration ((',' | NL+) declaration)* NL*)? ']';

type
	:	'(' type ')'											# ParenType
	|	'?'														# NullType
	|	type '?'												# NullableType
	|	Mut? objectType											# ObjectTypeType
	|	Name													# TypeReference
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
