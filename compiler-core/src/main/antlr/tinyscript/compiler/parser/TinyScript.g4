grammar TinyScript;

file: NL* (declaration ((',' | NL+) declaration)*)? NL* EOF;

declaration
	:	signature '=' expression												# FunctionDefinition
	|	Native signature ':' typeExpression										# NativeFunctionDeclaration
	|	'type' Name Mutable? '=' typeExpression									# TypeAliasDefinition
	|	Native 'type' Name Mutable?												# NativeTypeDeclaration
	|	'enum' Name '=' Name (NL* '|' Name)+									# EnumTypeDefinition
	;

signature
	:	(typeExpression '.')? Name Mutable? objectType?							# NameSignature
	|	(lhs=typeExpression)? OperatorSymbol Mutable? rhs=typeExpression		# OperatorSignature
	;

expression
	:	block																	# BlockExpression
	|	IntegerLiteral															# IntegerLiteralExpression
	|	FloatLiteral															# FloatLiteralExpression
	|	StringLiteral															# StringLiteralExpression
	|	object																	# ObjectExpression
	|	Name Mutable? object?													# NameCallExpression
	|	expression NL* '.' Name Mutable? object?								# DotNameCallExpression
	|	expression NL* '.' Mutable? object?										# AnonymousFunctionCallExpression
	|	OperatorSymbol Mutable? expression										# PrefixOperatorCallExpression
	|	lhs=expression NL* OperatorSymbol Mutable? NL* rhs=expression			# InfixOperatorCallExpression
	|	'if' NL* (block expression NL*)+ 'else' expression						# ConditionalExpression
	|	expression 'if' NL* (block expression NL*)+ 'else' expression			# ExprConditionalExpression // not sure yet.
	|	expression 'then' NL* expression										# SingleConditionalExpression
	|	Mutable? objectType? '->' NL* expression								# AnonymousFunctionExpression
	;

block: '(' NL* ((blockStatement (',' | NL+))* expression NL*)? ')';

blockStatement: (Name '=')? expression;

object: '[' NL* (objectStatement ((',' | NL+) objectStatement)*)? NL* ']';

objectStatement
	:	Name '=' expression														# ObjectFieldDefinition
	|	'&' expression															# ObjectInheritStatement
	;

typeExpression
	:	'(' NL* (typeExpression NL*)? ')'										# ParenTypeExpression
	|	Mutable? objectType? '->' typeExpression								# FunctionTypeExpression
	|	objectType																# ObjectTypeExpression
	|	Name Mutable?															# TypeReferenceExpression
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
Mutable: '!';

IntegerLiteral: [0-9]+;

FloatLiteral: [0-9]+ '.' [0-9]+;

StringLiteral: '"' StringCharacter* '"';

fragment
StringCharacter: ~["\\];

OperatorSymbol: '+' | '-' | '*' | '/' | '^' | '%' | '==' | '!=' | '<' | '>' | '&&' | '||';

Name: [a-zA-Z$_] [a-zA-Z$_0-9]*;

NL: [\r\n]+;

WS: [ \t\u000C]+ -> skip;

Comment: '/*' .*? '*/' -> channel(HIDDEN);

LineComment: '//' ~[\r\n]* -> channel(HIDDEN);
