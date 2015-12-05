package io.kanaka.e2e.plugin.checker

import sbt._
import scala.io.Source
/**
  * @author Valentin Kasas
  */
object ConsistencyChecker {

  type KeyUsagesByFilePath = Map[String, List[KeyUsage]]

  val readUsages = (path: File) => Source.fromFile(path).getLines().map(KeyUsage.fromCSV).flatten

  val groupByFile = (rawUsages: Iterator[KeyUsage]) =>
    rawUsages.foldLeft(Map.empty[String, List[KeyUsage]]) {
      (m, k) =>
        val entry = k :: m.getOrElse(k.path, Nil).filterNot(_.applicationId < k.applicationId)
        m + (k.path -> entry)
    }

  val getUsedKeys = (byFile: KeyUsagesByFilePath) => byFile.values.map(_.map(_.key).toSet).fold(Set.empty)(_ ++ _)

  def definedKeys(source: File): Set[String] = {
    val  msgSource = new MessageSource {
      override def read: String = Source.fromFile(source).mkString
    }
    val parser = new MessagesParser(msgSource, source.getName)
    parser.parse.fold(
      err => throw new Exception(err),
      messages =>
        messages.map(_.key).toSet
    )
  }

  def undefinedKeys(usages: sbt.File, resources: Seq[sbt.File]): Map[sbt.File, Set[String]] = {
    val usedKeys = (readUsages andThen groupByFile andThen getUsedKeys)(usages)
    resources.filter(_.getName.startsWith("messages.")).map{ file =>
      file -> (usedKeys -- definedKeys(file))
    }.toMap
  }
}
