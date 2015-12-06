package io.kanaka.e2e.plugin.checker

import java.text.MessageFormat

import sbt._

import scala.io.Source
/**
  * @author Valentin Kasas
  */
case class Translation(key: String, nbParameters: Int)

object Translation {
  def fromMessage(message: Message): Translation = new Translation(message.key, new MessageFormat(message.pattern).getFormatsByArgumentIndex.length)
}
object ConsistencyChecker {

  type KeyUsagesByFile = Map[String, List[KeyUsage]]

  val readUsages = (path: File) => Source.fromFile(path).getLines().map(KeyUsage.fromCSV).flatten

  val groupByFile = (rawUsages: Iterator[KeyUsage]) =>
    rawUsages.foldLeft(Map.empty[String, List[KeyUsage]]) {
      (m, k) =>
        val entry = k :: m.getOrElse(k.path, Nil).filterNot(_.applicationId < k.applicationId)
        m + (k.path -> entry)
    }

  val getUsedKeys = (byFile: KeyUsagesByFile) => byFile.values.map(_.map(_.key).toSet).fold(Set.empty)(_ ++ _)

  def definedKeys(source: File): Set[Translation] = {
    val  msgSource = new MessageSource {
      override def read: String = Source.fromFile(source).mkString
    }
    val parser = new MessagesParser(msgSource, source.getName)
    parser.parse.fold(
      err => throw new Exception(err),
      _.map(Translation.fromMessage).toSet
    )
  }

  def verifyFile(file: File, usageData: KeyUsagesByFile): Set[TranslationProblem] = {
    val translations = definedKeys(file)
    val usages = usageData.values.flatMap(keyUsages => keyUsages.groupBy(_.key).mapValues(_.map(_.nbParameters).fold(0)(Math.max)).map(p => Translation(p._1, p._2))).toSet
    (usages -- translations).map { trans =>
      translations.find(_.key == trans.key).fold[TranslationProblem](MissingTranslation(trans.key))(t => WrongNumberOfArguments(trans.key, trans.nbParameters, t.nbParameters))
    }
  }

  def undefinedKeys(usages: File, resources: Seq[File]): Map[File, Set[TranslationProblem]] = {
    val usedKeys = (readUsages andThen groupByFile)(usages)
    resources.filter(_.getName.startsWith("messages")).map{ file =>
      file -> verifyFile(file, usedKeys)
    }.toMap
  }
}

sealed trait TranslationProblem
case class MissingTranslation(key: String) extends TranslationProblem
case class WrongNumberOfArguments(key: String, inCode: Int, inMessages: Int) extends TranslationProblem