var myConstant = "initial value";
var myVariable = "initial value";
var myStringOrNull = null;
var main = () => ((myStringOrNull = "foo"));
var multiplyByTwo = (n, ) => (n * 2);
var sayHello = () => (println("Hello", ));
var Animal = function () {
// this.name;
this.sayName = () => (println("my name is $name", ));
};
var Dog = function () {
(Animal).call(this);
this.bark = () => (println("woof", ));
};
var myDog = new (function () {
(Dog).call(this);
this.name = "Foo";
})();
var myObject = new (function () {
this.foo = "bar";
this.abc = 123;
})();
var Scanner = function () {
// this.scan;
};
var Printer = function () {
// this.print;
};
var Copier = function () {
(function () {
(Scanner).call(this);
(Printer).call(this);
}).call(this);
this.copy = () => ((function () {
this.scan();
return this.print();
})());
};
var message = (
(true) ? ("Hello") :
"Hi"
);
var Point = function () {
// this.x;
// this.y;
};
var TODO = new (function () {
(Point).call(this);
this.x = 1;
this.y = 2;
})();
