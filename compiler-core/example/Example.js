var myConstant = "initial value";
var myVariable = "initial value";
var myStringOrNull = null;
var main = () => ((myStringOrNull = "foo"));
var multiplyByTwo = (n,) => (n * 2);
var sayHello = () => (println());
var Animal = function () {
// this.name;
this.sayName = () => (println());
};
var Dog = function () {
(Animal).call(this);
this.bark = () => (println());
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
var message = TODO;
