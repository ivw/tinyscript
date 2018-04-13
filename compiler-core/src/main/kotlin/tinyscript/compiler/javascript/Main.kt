package tinyscript.compiler.javascript

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import tinyscript.compiler.ast.AnalysisException
import tinyscript.compiler.ast.analyse
import tinyscript.compiler.scope.Scope
import tinyscript.compiler.scope.builtInEntities
import tinyscript.compiler.ast.parser.TinyScriptLexer
import tinyscript.compiler.ast.parser.TinyScriptParser
import tinyscript.compiler.util.IndentedWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun compileTinyScriptToJavascript(readPath: Path, writePath: Path) {
	println("Reading file '${readPath.fileName}'\n")
	val inputCharStream = CharStreams.fromPath(readPath)

	println("Starting parsing")
	val lexer = TinyScriptLexer(inputCharStream)
	val parser = TinyScriptParser(CommonTokenStream(lexer))
	val fileCtx = parser.file()
	println("Parsing done\n")

	if (parser.numberOfSyntaxErrors > 0)
		throw RuntimeException("parsing failed")

	println("Starting analysis")
	val statementList = fileCtx.statementList().analyse(
		Scope(null, builtInEntities)
	)
	if (statementList.hasImpureImperativeStatement)
		throw AnalysisException("file scope can not have impure imperative statements")
	println("Analysis done\n")

	Files.newBufferedWriter(writePath, StandardCharsets.UTF_8).use { writer ->
		val indentedWriter = IndentedWriter(writer)
		statementList.writeJS(indentedWriter)
		println("Written to file '${writePath.fileName}'\n")
	}
}

fun main(args: Array<String>) {
	if (args.isEmpty()) throw IllegalArgumentException("specify a file to compile")

	val tinyScriptFileName = args[0]
	if (!tinyScriptFileName.endsWith(".tiny")) throw IllegalArgumentException("must be a .tiny file")

	val outputJsFileName = tinyScriptFileName.removeSuffix(".tiny") + ".js"

	compileTinyScriptToJavascript(
		Paths.get(tinyScriptFileName),
		Paths.get(outputJsFileName)
	)
}
