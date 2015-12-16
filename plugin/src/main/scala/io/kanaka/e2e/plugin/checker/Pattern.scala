package io.kanaka.e2e.plugin.checker

import java.text.MessageFormat

import scala.annotation.tailrec

/**
  * @author Valentin Kasas
  */



object Pattern {

  type Err = String

  val escapableCharacters = Set('\'', '{', '}')

  def suspiciousQuotes(pattern: String): Option[ConsistencyProblem] = {
    // Single quotes are a escaping meta-character in MessageFormat DSL
    // Users often overlook that fact and forget to double their single quotes
    // For example :
    //    my.key = C'est la fête
    // Would render as
    //    Cest la fête
    // Although this is probably not what the user meant.
    // We try to detect such misuses and raise a warning to the user.
    def escaping(str: List[Char], nbRead: Int, warnings: List[Int]): List[Int] = str match {
      case c :: t if escapableCharacters.contains(c) => reading(t, nbRead + 1, warnings)
      case c :: t => reading(t, nbRead + 1, nbRead :: warnings)
      case Nil => (nbRead :: warnings).reverse
    }
    @tailrec
    def reading(str: List[Char], nbRead: Int, warnings: List[Int]):List[Int] = str match {
      case '\'' :: tail => escaping(tail, nbRead + 1, warnings)
      case c :: tail => reading(tail, nbRead + 1, warnings)
      case Nil => warnings.reverse

    }

    val indices = reading(pattern.toList, -1, Nil)
    if (indices.isEmpty) None
    else Some(SuspiciousQuotesInPattern(pattern, indices))
  }

  def parse(pattern: String): Either[ConsistencyProblem, Pattern] = {
    try {
      val parsed = new MessageFormat(pattern)
      val nbParameters = parsed.getFormatsByArgumentIndex.length
      Right(Pattern(pattern, nbParameters))
    } catch {
      case e: IllegalArgumentException => Left(PatternParseError(pattern, e.getMessage))
    }
  }

}

case class Pattern(raw: String, nbParameters: Int)
