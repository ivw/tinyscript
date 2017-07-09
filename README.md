# TinyScript

A minimal, safe programming language that compiles to JavaScript.

Work in progress.

## Usage
Java 8 is required.

Download the compiler as an executable jar (tinyc.jar) from the [latest release](https://github.com/ivw/tinyscript/releases/latest).

To compile a TinyScript file ([example TinyScript file](https://raw.githubusercontent.com/ivw/tinyscript/master/compiler-core/example/Example.tiny)), run:

```
java -jar tinyc.jar Example.tiny
```

On successful compilation, a JavaScript file will be created with the same name and location.
