package tinyscript.compiler.core

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Token
import tinyscript.compiler.core.parser.TinyScriptLexer
import tinyscript.compiler.core.parser.TinyScriptParser
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun printError(token: Token, message: String) {
	System.err.printf("line %d:%d: %s\n", token.line, token.charPositionInLine, message)
}

fun writeTinyScriptToJavascript(readPath: Path, writePath: Path) {
	println("Reading file '${readPath.fileName}'\n")
	val inputCharStream = CharStreams.fromPath(readPath)

	println("Starting parsing")
	val parser = TinyScriptParser(CommonTokenStream(TinyScriptLexer(inputCharStream)))
	val fileCtx = parser.file()
	println("Parsing done\n")

	println("Starting analysis")
	val analysisVisitor = AnalysisVisitor()
	analysisVisitor.visitFile(fileCtx)
	analysisVisitor.finishDeferredAnalyses()
	println("Analysis done\n")

	Files.newBufferedWriter(writePath, StandardCharsets.UTF_8).use { writer ->
		val javascriptGenerator = JavascriptGenerator(writer, analysisVisitor.typeMap)
		javascriptGenerator.writeFile(fileCtx)
		println("Written to file '${writePath.fileName}'\n")
	}
}

fun main(args: Array<String>) {
	if (args.isEmpty()) throw IllegalArgumentException("specify a file to compile")

	val tinyScriptFileName = args[0]
	if (!tinyScriptFileName.endsWith(".tiny")) throw IllegalArgumentException("must be a .tiny file")

	val outputJsFileName = tinyScriptFileName.removeSuffix(".tiny") + ".js"

	writeTinyScriptToJavascript(
			Paths.get(tinyScriptFileName),
			Paths.get(outputJsFileName)
	)
}
