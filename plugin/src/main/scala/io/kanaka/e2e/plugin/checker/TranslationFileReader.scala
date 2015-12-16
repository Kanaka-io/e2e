package io.kanaka.e2e.plugin.checker

import sbt._

import scala.io.Source

/**
  * @author Valentin Kasas
  */

object TranslationFileReader {

  case class FileMessageSource(source: File) extends MessageSource {
    override def read: String = Source.fromFile(source).mkString
  }

  def loadMessages(source: File): Either[FileParseError, Seq[Message]] = {
    val parser = new MessagesParser(FileMessageSource(source), source.getName)
    parser.parse.fold(
      err => Left(FileParseError(source, err)),
      messages => Right(messages)
    )
  }

}
