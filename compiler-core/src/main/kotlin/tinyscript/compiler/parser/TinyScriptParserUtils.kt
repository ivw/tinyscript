package tinyscript.compiler.parser

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import java.util.*

class ParsingFailedException(override val message: String?) : RuntimeException(message)

private class FullANTLRErrorListener : ANTLRErrorListener {
	override fun reportAttemptingFullContext(recognizer: Parser?, dfa: DFA?, startIndex: Int, stopIndex: Int, conflictingAlts: BitSet?, configs: ATNConfigSet?) {
		throw ParsingFailedException("attempting full context")
	}

	override fun syntaxError(recognizer: Recognizer<*, *>?, offendingSymbol: Any?, line: Int, charPositionInLine: Int, msg: String?, e: RecognitionException?) {
		throw ParsingFailedException("line $line:$charPositionInLine $msg")
	}

	override fun reportAmbiguity(recognizer: Parser?, dfa: DFA?, startIndex: Int, stopIndex: Int, exact: Boolean, ambigAlts: BitSet?, configs: ATNConfigSet?) {
		throw ParsingFailedException("ambiguity")
	}

	override fun reportContextSensitivity(recognizer: Parser?, dfa: DFA?, startIndex: Int, stopIndex: Int, prediction: Int, configs: ATNConfigSet?) {
		throw ParsingFailedException("context sensitivity")
	}
}

fun parseFile(input: CharStream): TinyScriptParser.FileContext {
	val errorListener = FullANTLRErrorListener()

	val lexer = TinyScriptLexer(input)
	lexer.removeErrorListeners()
	lexer.addErrorListener(errorListener)

	val parser = TinyScriptParser(CommonTokenStream(lexer))
	parser.removeErrorListeners()
	parser.addErrorListener(errorListener)

	return parser.file()
}
