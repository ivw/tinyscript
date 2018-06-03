grammar TinyScript;

file: NL* (declaration ((',' | NL+) declaration)*)? NL* EOF;

declaration
	:	signature '=' expression												# ValueDefinition
	|	Native signature ':' typeExpression										# NativeDeclaration
	|	'type' Name '=' typeExpression											# TypeAliasDefinition
	|	Native 'type' Name														# NativeTypeDeclaration
	|	'enum' Name '=' Name (NL* '|' Name)+									# EnumTypeDefinition
	;

signature
	:	Name																	# FieldSignature
	|	(typeExpression '.')? Name objectType									# FunctionSignature
	|	(lhs=typeExpression)? OperatorSymbol rhs=typeExpression					# OperatorSignature
	;

expression
	:	block																	# BlockExpression
	|	IntegerLiteral															# IntegerLiteralExpression
	|	FloatLiteral															# FloatLiteralExpression
	|	StringLiteral															# StringLiteralExpression
	|	object																	# ObjectExpression
	|	Name																	# FieldRefExpression
	|	expression NL* '.' Name													# ObjectFieldRefExpression
	|	Name object																# FunctionCallExpression
	|	expression NL* '.' Name object											# DotFunctionCallExpression
	|	OperatorSymbol NL* rhs=expression										# PrefixOperatorCallExpression
	|	expression NL* '.' object?												# AnonymousFunctionCallExpression
	|	lhs=expression NL* OperatorSymbol NL* rhs=expression					# InfixOperatorCallExpression
	|	'if' NL* (block expression NL*)+ 'else' expression						# ConditionalExpression
	|	expression 'if' NL* (block expression NL*)+ 'else' expression			# ExprConditionalExpression // not sure yet.
	|	expression 'then' NL* expression										# SingleConditionalExpression
	|	objectType? '->' NL* expression											# AnonymousFunctionExpression
	;

block: '(' NL* ((blockStatement (',' | NL+))* expression NL*)? ')';

blockStatement: (Name '=')? expression;

object: '[' NL* (objectStatement ((',' | NL+) objectStatement)*)? NL* ']';

objectStatement
	:	Name '=' expression														# ObjectFieldDeclaration
	|	'&' expression															# ObjectInheritStatement
	;

typeExpression
	:	'(' NL* (typeExpression NL*)? ')'										# ParenTypeExpression
	|	objectType? '->' typeExpression											# FunctionTypeExpression
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
