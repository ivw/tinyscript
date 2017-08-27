var myConstant = "initial value";
var myVariable = "initial value";
var myStringOrNull = null;
var main = () => ((myStringOrNull = "foo"));
var multiplyByTwo = (n, ) => ($op(n, 2));
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
  (Scanner).call(this);
  (Printer).call(this);
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
var $op = ($0, $1) => (new (function () {
  (Point).call(this);
  this.x = $op($0.x, $1.x);
  this.y = $op($0.y, $1.y);
})());
var myPoint = $op(new (function () {
  (Point).call(this);
  this.x = 1;
  this.y = 2;
})(), new (function () {
  (Point).call(this);
  this.x = 1;
  this.y = 2;
})());
