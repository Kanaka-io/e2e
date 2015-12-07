package io.kanaka.e2e.plugin.checker

import sbt.File

import scala.io.Source
import scala.util.Try

/**
  * @author Valentin Kasas
  */
case class UsageData(usages: List[KeyUsage]) {
  lazy val keys: Set[String] = byKey.keySet
  lazy val byKey: Map[String, List[KeyUsage]] = usages.groupBy(_.key)
  def minimumNbParameters(key: String) = byKey.getOrElse(key, Nil).foldLeft(0)((max, ku) => Math.max(max, ku.nbParameters))
}

object UsageData {
  def load(file: File): Try[UsageData] = Try {
    val rawUsages = Source.fromFile(file).getLines().map(KeyUsage.fromCSV).flatten.toList

    // There can be different sets of usage records for the same source file (generated by different builds)
    // We have to filter the raw list to keep only the most recent set for each source file
    val usagesBySourceFile = rawUsages.groupBy(_.path)

    val mostRecentBySourceFile = usagesBySourceFile.mapValues{ usages =>
      usages.foldLeft((List.empty[KeyUsage], 0L)){
        case ((list, maxApplicationId), usage) if usage.applicationId > maxApplicationId =>
          (usage :: Nil, usage.applicationId)
        case ((list, maxApplicationId), usage) if usage.applicationId == maxApplicationId =>
          (usage :: list, maxApplicationId)
        case (p, _) => p
      }._1
    }
    UsageData(mostRecentBySourceFile.values.flatten.toList)
  }
}