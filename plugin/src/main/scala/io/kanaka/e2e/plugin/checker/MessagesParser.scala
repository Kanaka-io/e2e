package io.kanaka.e2e.plugin.checker

import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.{CharSequenceReader, Positional}

/*
    The code below is shamelessly copied from Play sourcecode.
 */

trait MessageSource {
  def read: String
}

case class Message(key: String, pattern: String, source: MessageSource, sourceName: String) extends Positional


/**
  * Message file Parser.
  */
class MessagesParser(messageSource: MessageSource, messageSourceName: String) extends RegexParsers {

  case class Comment(msg: String)

  override def skipWhitespace = false
  override val whiteSpace = """^[ \t]+""".r

  def namedError[A](p: Parser[A], msg: String) = Parser[A] { i =>
    p(i) match {
      case Failure(_, in) => Failure(msg, in)
      case o => o
    }
  }

  val end = """^\s*""".r
  val newLine = namedError("\r".? ~> "\n", "End of line expected")
  val ignoreWhiteSpace = opt(whiteSpace)
  val blankLine = ignoreWhiteSpace <~ newLine ^^ { case _ => Comment("") }

  val comment = """^#.*""".r ^^ { case s => Comment(s) }

  val messageKey = namedError("""^[a-zA-Z0-9_.-]+""".r, "Message key expected")

  val messagePattern = namedError(
    rep(
      ("""\""" ^^ (_ => "")) ~> ( // Ignore the leading \
        "\r".? ~> "\n" ^^ (_ => "") | // Ignore escaped end of lines \
          "n" ^^ (_ => "\n") | // Translate literal \n to real newline
          """\""" | // Handle escaped \\
          "^.".r ^^ ("""\""" + _)
        ) |
        "^.".r // Or any character
    ) ^^ { case chars => chars.mkString },
    "Message pattern expected"
  )

  val message = ignoreWhiteSpace ~ messageKey ~ (ignoreWhiteSpace ~ "=" ~ ignoreWhiteSpace) ~ messagePattern ^^ {
    case (_ ~ k ~ _ ~ v) => Message(k, v.trim, messageSource, messageSourceName)
  }

  val sentence = (comment | positioned(message)) <~ newLine

  val parser = phrase((sentence | blankLine).* <~ end) ^^ {
    case messages => messages.collect {
      case m @ Message(_, _, _, _) => m
    }
  }

  def parse: Either[String, Seq[Message]] = {
    parser(new CharSequenceReader(messageSource.read + "\n")) match {
      case Success(messages, _) => Right(messages)
      case NoSuccess(message, in) => Left("Configuration error")
    }
  }

}
