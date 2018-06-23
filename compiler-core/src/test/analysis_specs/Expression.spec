//// empty block expression (`any` expression)

foo = ()

////

foo = (

)

////
// error = OperatorSignatureNotFoundException

foo = 2 * ()

//// block without statements

foo = (3) * 2

////

foo = (

	3

) * 2

////
// error = OperatorSignatureNotFoundException

foo = (()) * 2

////
// error = FunctionMutableOutputException

foo = (new intBox)

////

foo = (((((3))) * (2)))

//// block with statements

total = (
	n = new intBox
	n *! 2
	n.get!
) * 2

////

total = ( n = 2, n * n ) * 2

//// IntegerLiteralExpression

a = 1

//// FloatLiteralExpression

a = 1.0

//// StringLiteralExpression

foo = "bar"
