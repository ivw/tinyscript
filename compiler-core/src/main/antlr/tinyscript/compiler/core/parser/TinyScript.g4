grammar TinyScript;

@header {package tinyscript.compiler.core.parser;}

// start
file: NL* statementList? NL* EOF;

statementList: statement ((',' | NL+) statement)*;

statement
	:	signature '=' expression												# ValueDeclaration
	|	'native' signature ':' typeExpression									# NativeDeclaration
	|	'type' Name '=' typeExpression											# TypeAliasDeclaration
	|	'enum' Name '{' NL* Name ((',' | NL+) Name)* NL* '}'					# EnumTypeDeclaration
	|	expression																# ExpressionStatement
	;

signature
	:	(typeExpression '.')? Name Impure? objectType?							# NameSignature
	|	(lhs=typeExpression)? OperatorSymbol Impure? rhs=typeExpression			# OperatorSignature
	;

expression
	:	block																	# BlockExpression
	|	IntegerLiteral															# IntegerLiteralExpression
	|	FloatLiteral															# FloatLiteralExpression
	|	StringLiteral															# StringLiteralExpression
	|	'this'																	# ThisExpression
	|	object																	# ObjectExpression
	|	Name Impure? object?													# NameReferenceExpression
	|	expression NL* '.' Name? Impure? object?								# DotReferenceExpression
	|	OperatorSymbol Impure? expression										# PrefixOperatorCallExpression
	|	expression NL* OperatorSymbol Impure? NL* expression					# InfixOperatorCallExpression
	|	'if' NL* (block expression NL*)+ 'else' expression						# ConditionalExpression
	|	expression 'if' NL* (block expression NL*)+ 'else' expression			# ExprConditionalExpression // not sure yet.
	|	expression 'then' NL* expression										# SingleConditionalExpression
	|	Impure? objectType? '->' NL* expression									# FunctionExpression
	;

block: '(' NL* (statementList (',' | NL+))? expression NL* ')';

object: '[' NL* (objectStatement ((',' | NL+) objectStatement)*)? NL* ']';

objectStatement
	:	Name '=' expression														# FieldDeclaration
	|	'&' expression															# InheritStatement
	;

typeExpression
	:	'(' typeExpression ')'													# ParenTypeExpression
	|	Impure? objectType? '->' typeExpression									# FunctionTypeExpression
	|	objectType																# ObjectTypeExpression
	|	Name																	# TypeReferenceExpression
	|	typeExpression block													# DependentTypeExpression
	;

objectType: '[' NL* (objectTypeStatement ((',' | NL+) objectTypeStatement)* NL*)? ']';

objectTypeStatement
	:	Name ':' typeExpression													# ObjectTypeField
	|	'&' Name																# ObjectTypeInheritStatement
	;

// LEXER TOKENS

Private: 'private';
Override: 'override';
Impure: '!';

IntegerLiteral: [0-9]+;

FloatLiteral: [0-9]+ '.' [0-9]+;

StringLiteral: '"' StringCharacter* '"';

fragment
StringCharacter: ~["\\];

OperatorSymbol: '+' | '-' | '*' | '/' | '^' | '%' | '==' | '!=';

Name: [a-zA-Z$_] [a-zA-Z$_0-9]*;

NL: [\r\n]+;

WS: [ \t\u000C]+ -> skip;

Comment: '/*' .*? '*/' -> channel(HIDDEN);

LineComment: '//' ~[\r\n]* -> channel(HIDDEN);
