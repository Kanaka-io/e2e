package io.kanaka.e2e.plugin.checker

import java.text.MessageFormat

import sbt._

import scala.io.Source
/**
  * @author Valentin Kasas
  */
case class Translation(key: String, pattern: String, nbParameters: Int, line: Int)

object Translation {
  def fromMessage(message: Message): Translation = new Translation(message.key, message.pattern, new MessageFormat(message.pattern).getFormatsByArgumentIndex.length, message.pos.line)
}
object ConsistencyChecker {

  type KeyUsagesByFile = Map[String, List[KeyUsage]]

  def loadTranslations(source: File): Set[Translation] = {
    val  msgSource = new MessageSource {
      override def read: String = Source.fromFile(source).mkString
    }
    val parser = new MessagesParser(msgSource, source.getName)
    parser.parse.fold(
      err => throw new Exception(err),
      _.map(Translation.fromMessage).toSet
    )
  }

  def verifyFile(file: File, usageData: UsageData): Set[TranslationProblem] = {
    val translations = loadTranslations(file)
    usageData.keys.flatMap { key =>
      val expectedNbParameters = usageData.minimumNbParameters(key)
      translations.find(_.key == key) match {
        case None =>
          Some(MissingTranslation(key))
        case Some(Translation(_, pattern, nbParams, line)) if nbParams < expectedNbParameters =>
          Some(WrongNumberOfArguments(key, pattern, expectedNbParameters, nbParams, line))
        case _ =>
          None
      }
    }
  }

  def verifyAllTranslationFiles(usages: File, resources: Seq[File]): Map[File, Set[TranslationProblem]] = {
    val usageData = UsageData.load(usages).get
    resources.filter(_.getName.startsWith("messages")).map{ file =>
      file -> verifyFile(file, usageData)
    }.toMap
  }
}

sealed trait TranslationProblem
case class MissingTranslation(key: String) extends TranslationProblem
case class WrongNumberOfArguments(key: String, pattern: String, inCode: Int, inMessages: Int, line: Int) extends TranslationProblem