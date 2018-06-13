package tinyscript.compiler.javascript

import org.antlr.v4.runtime.CharStreams
import tinyscript.compiler.ast.analyse
import tinyscript.compiler.parser.parseFile
import tinyscript.compiler.stdlib.StandardLibrary
import tinyscript.compiler.util.IndentedWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun compileTinyScriptToJavascript(readPath: Path, writePath: Path) {
	println("Reading and parsing file '${readPath.fileName}'")
	val fileCtx = parseFile(CharStreams.fromPath(readPath))
	println("Parsing done\n")

	println("Starting analysis")
	val declarationList = fileCtx.declaration().analyse(StandardLibrary.scope)
	println("Analysis done\n")

	Files.newBufferedWriter(writePath, StandardCharsets.UTF_8).use { writer ->
		val indentedWriter = IndentedWriter(writer)
		declarationList.writeJS(indentedWriter)
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
