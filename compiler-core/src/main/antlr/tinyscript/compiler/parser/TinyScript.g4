grammar TinyScript;

file: NL* (declaration ((',' | NL+) declaration)*)? NL* EOF;

declaration
	:	signature '=' expression												# FunctionDefinition
	|	Native signature ':' typeExpression										# NativeFunctionDeclaration
	|	'type' TypeName Impure? '=' typeExpression								# TypeAliasDefinition
	|	Native 'type' TypeName Impure?											# NativeTypeDeclaration
	|	'enum' TypeName Impure? '=' ValueName (NL* '|' ValueName)+				# EnumTypeDefinition
	;

signature
	:	(typeExpression '.')? ValueName Impure? objectType?						# NameSignature
	|	(lhs=typeExpression)? OperatorSymbol Impure? rhs=typeExpression			# OperatorSignature
	;

expression
	:	block																	# BlockExpression
	|	IntegerLiteral															# IntegerLiteralExpression
	|	FloatLiteral															# FloatLiteralExpression
	|	StringLiteral															# StringLiteralExpression
	|	object																	# ObjectExpression
	|	ValueName Impure? object?												# NameCallExpression
	|	expression NL* '.' ValueName Impure? object?							# DotNameCallExpression
	|	expression NL* '.' Impure? object?										# AnonymousFunctionCallExpression
	|	OperatorSymbol Impure? expression										# PrefixOperatorCallExpression
	|	lhs=expression NL* OperatorSymbol Impure? NL* rhs=expression			# InfixOperatorCallExpression
	|	'if' NL* (block expression NL*)+ 'else' expression						# ConditionalExpression
	|	expression 'if' NL* (block expression NL*)+ 'else' expression			# ExprConditionalExpression // not sure yet.
	|	expression 'then' NL* expression										# SingleConditionalExpression
	|	Impure? objectType? '->' NL* expression									# AnonymousFunctionExpression
	;

block: '(' NL* ((blockStatement (',' | NL+))* expression NL*)? ')';

blockStatement: (ValueName '=')? expression;

object: '[' NL* (objectStatement ((',' | NL+) objectStatement)*)? NL* ']';

objectStatement
	:	ValueName '=' expression												# ObjectFieldDefinition
	|	'&' expression															# ObjectInheritStatement
	;

typeExpression
	:	'(' NL* (typeExpression NL*)? ')'										# ParenTypeExpression
	|	Impure? objectType? '->' typeExpression									# FunctionTypeExpression
	|	objectType																# ObjectTypeExpression
	|	TypeName Impure?														# TypeReferenceExpression
	|	typeExpression block													# DependentTypeExpression
	;

objectType: '[' NL* (objectTypeStatement ((',' | NL+) objectTypeStatement)* NL*)? ']';

objectTypeStatement
	:	ValueName ':' typeExpression											# ObjectTypeFieldDeclaration
	|	'&' TypeName Impure?													# ObjectTypeInheritStatement
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

OperatorSymbol: '+' | '-' | '*' | '/' | '^' | '%' | '==' | '!=' | '<' | '>' | '&&' | '||';

ValueName: [a-z] [a-zA-Z0-9]*;

TypeName: [A-Z] [a-zA-Z0-9]*;

NL: [\r\n]+;

WS: [ \t\u000C]+ -> skip;

Comment: '/*' .*? '*/' -> channel(HIDDEN);

LineComment: '//' ~[\r\n]* -> channel(HIDDEN);
