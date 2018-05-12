grammar TinyScript;

@header {package tinyscript.compiler.parser;}

file: NL* statementList? NL* EOF;

statementList: statement ((',' | NL+) statement)*;

statement
	:	(Name '=')? expression													# ImperativeStatement
	|	signature '=>' expression												# FunctionDefinition
	|	Native signature ':' typeExpression										# NativeDeclaration
	|	'type' Name '=' typeExpression											# TypeAliasDefinition
	|	Native 'type' Name														# NativeTypeDeclaration
	|	'enum' Name '=' Name (NL* '|' Name)+									# EnumTypeDefinition
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
	|	object																	# ObjectExpression
	|	Name Impure? object?													# NameReferenceExpression
	|	expression NL* '.' Name Impure? object?								# DotNameReferenceExpression
	|	expression NL* '.' Impure? object?										# AnonymousFunctionCallExpression
	|	OperatorSymbol Impure? expression										# PrefixOperatorCallExpression
	|	lhs=expression NL* OperatorSymbol Impure? NL* rhs=expression			# InfixOperatorCallExpression
	|	'if' NL* (block expression NL*)+ 'else' expression						# ConditionalExpression
	|	expression 'if' NL* (block expression NL*)+ 'else' expression			# ExprConditionalExpression // not sure yet.
	|	expression 'then' NL* expression										# SingleConditionalExpression
	|	Impure? objectType? '->' NL* expression									# AnonymousFunctionExpression
	;

block: '(' NL* ((statementList (',' | NL+))? expression NL*)? ')';

object: '[' NL* (objectStatement ((',' | NL+) objectStatement)*)? NL* ']';

objectStatement
	:	Name '=' expression														# ObjectFieldDeclaration
	|	'&' expression															# ObjectInheritStatement
	;

typeExpression
	:	'(' NL* (typeExpression NL*)? ')'										# ParenTypeExpression
	|	Impure? objectType? '->' typeExpression									# FunctionTypeExpression
	|	objectType																# ObjectTypeExpression
	|	Name																	# TypeReferenceExpression
	|	typeExpression block													# DependentTypeExpression
	;

objectType: '[' NL* (objectTypeStatement ((',' | NL+) objectTypeStatement)* NL*)? ']';

objectTypeStatement
	:	Name ':' typeExpression													# ObjectTypeFieldDeclaration
	|	'&' Name																# ObjectTypeInheritStatement
	;

// LEXER TOKENS

Private: 'private';
Native: 'native';
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
