package io.kanaka.e2e.plugin.checker

import sbt._

/**
  * @author Valentin Kasas
  */


trait ConsistencyProblem

case class FileParseError(file: File, err: String) extends ConsistencyProblem
case class MissingTranslation(key: String) extends ConsistencyProblem
case object UnusedTranslation extends ConsistencyProblem
case class WrongNumberOfArguments(pattern: String, inCode: Int, inMessages: Int) extends ConsistencyProblem
case class SuspiciousQuotesInPattern(pattern: String, indices: List[Int]) extends ConsistencyProblem
case class PatternParseError(raw: String, err: String) extends ConsistencyProblem

case class FileAnalysisReport(file: File, content: Seq[RichMessage], missingKeys: Set[String]) {
  def nbProblems = content.map(_.problems.size).sum + missingKeys.size
}

case class RichMessage(message: Message, problems: Seq[ConsistencyProblem])

object ConsistencyChecker {

  type ConsistencyCheck = (Seq[Either[FileParseError, FileAnalysisReport]], UsageData)

  def verifyFile(file: File, usageData: UsageData): Either[FileParseError, FileAnalysisReport]= {
    TranslationFileReader.loadMessages(file).right.map(
      messages => {
        val missingKeys = usageData.keys -- messages.map(_.key).toSet
        val content = messages.map(
          message => RichMessage(message, validateMessage(message, usageData.byKey.getOrElse(message.key, Nil)))
        )
        FileAnalysisReport(file, content, missingKeys)
      }
    )
  }

  def validateMessage(message: Message, usages: List[KeyUsage]): Seq[ConsistencyProblem] = {
    Pattern.parse(message.pattern).fold(
      _ :: Nil,
      pattern => {
        val suspiciousQuotes = Pattern.suspiciousQuotes(pattern.raw).toList
        val minimumNbParameters = usages.map(_.nbParameters).fold(0)(Math.max)
        val wrongNumberOfArguments = if (pattern.nbParameters < minimumNbParameters) WrongNumberOfArguments(pattern.raw, minimumNbParameters, pattern.nbParameters) :: Nil else Nil
        suspiciousQuotes ++ wrongNumberOfArguments
      }
    )
  }

  def verifyAllTranslationFiles(usages: File, resources: Seq[File]): ConsistencyCheck = {
    val usageData = UsageData.load(usages).get
    (resources.filter(_.getName.startsWith("messages")).map(verifyFile(_, usageData)), usageData)
  }
}

