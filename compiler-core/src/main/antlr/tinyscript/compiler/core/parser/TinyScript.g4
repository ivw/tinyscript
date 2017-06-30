grammar TinyScript;

@header {package tinyscript.compiler.core.parser;}

// start
file: NL* (declaration ((',' | NL+) declaration)* NL*)? EOF;

declaration
	:	symbol ':' type											# AbstractDeclaration
	|	symbol (':' type)? '=' NL* expression					# ConcreteDeclaration
	|	expression												# ImplicitDeclaration
	;

symbol: Private? Override? Hash? (type '.')? Name;

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
	|	object													# ObjectExpression // this is basically a shorthand for `Object[foo = 123]`
	|	expression object										# ObjectOrCallExpression // this is for both calling functions and for instantiating objects.
	|	expression '&' expression								# ClassMergeExpression
	|	Operator expression										# PrefixOperatorCallExpression
	|	expression NL* Operator NL* expression					# InfixOperatorCallExpression
	|	'class' object											# ClassExpression
	|	expression 'class' object								# ExtendClassExpression
	|	'if' NL* (block expression NL*)+ 'else' expression		# ConditionalExpression
	|	Name '<-' expression									# ReassignmentExpression
	|	expression NL* '.' Name '<-' expression					# DotReassignmentExpression
	|	object? '->' NL* expression								# FunctionExpression // this rule is at the bottom precedence, because everything inside the rhs expression goes first
	;

block: '(' NL* (declaration (';' | NL+))* expression NL* ')';

object: '[' NL* (declaration ((',' | NL+) declaration)* NL*)? ']';

type
	:	'(' type ')'											# ParenType
	|	objectType? '->' type									# FunctionType
	|	'?'														# NullType
	|	type '?'												# NullableType
	|	objectType												# ObjectTypeType
	|	Name													# TypeReference
	|	type '&' type											# MergedObjectType
	;

objectType: '[' NL* (objectTypeField ((',' | NL+) objectTypeField)* NL*)? ']';

objectTypeField: symbol ':' type;

// LEXER TOKENS

Private: 'private';
Override: 'override';
Hash: '#';

IntegerLiteral: [0-9]+;

FloatLiteral: [0-9]+ '.' [0-9]+;

BooleanLiteral: 'true' | 'false';

StringLiteral: '"' StringCharacter* '"';

fragment
StringCharacter: ~["\\];

Operator: '+' | '-' | '*' | '/' | '^' | '%' | '!' | '==' | '!=';

Name: [a-zA-Z] [a-zA-Z0-9]*;

NL: [\r\n]+;

WS: [ \t\u000C]+ -> skip;

Comment: '/*' .*? '*/' -> channel(HIDDEN);

LineComment: '//' ~[\r\n]* -> channel(HIDDEN);
