grammar TinyScript;

@header {package tinyscript.compiler.core.parser;}

// start
file: NL* statementList? NL* EOF;

statementList: statement ((',' | NL+) statement)*;

statement
	:	(typeExpression '.')? Name objectType? Impure? initializer				# NameDeclaration
	|	(lhs=typeExpression)? Operator Impure? rhs=typeExpression initializer	# OperatorDeclaration
	|	'type' Name '=' typeExpression											# TypeAliasDeclaration
	|	'enum' Name '{' NL* Name ((',' | NL+) Name)* NL* '}'					# EnumTypeDeclaration
	|	expression																# ExpressionStatement
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
	|	object																	# ObjectExpression
	|	Name object? Impure?													# NameReferenceExpression
	|	expression NL* '.' Name object? Impure?									# DotNameReferenceExpression
	|	Operator Impure? expression												# PrefixOperatorCallExpression
	|	expression NL* Operator Impure? NL* expression							# InfixOperatorCallExpression
	|	'if' NL* (block expression NL*)+ 'else' expression						# ConditionalExpression
	|	expression 'if' NL* (block expression NL*)+ 'else' expression			# ExprConditionalExpression // not sure yet.
	|	expression 'then' NL* expression										# SingleConditionalExpression
	|	object? Impure? '->' NL* expression										# FunctionExpression
	;

block: '(' NL* (statementList (',' | NL+))? expression NL* ')';

object: '[' NL* (objectStatement ((',' | NL+) objectStatement)*)? NL* ']';

objectStatement
    :   Name initializer                                # FieldDeclaration
    |   '&' expression															# InheritStatement
    ;

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
	:	Name ':' typeExpression								# NameObjectTypeField
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
