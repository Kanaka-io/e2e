package io.kanaka.e2e.plugin.checker

import java.io.{FileWriter, PrintWriter}
import java.text.MessageFormat

import sbt._

import scala.annotation.tailrec
import scala.io.Source
import scala.util.parsing.input.Position
import scala.util.{Failure, Success, Try}

/**
  * @author Valentin Kasas
  */

object I18NFixer {

  sealed trait Command
  case class DefineTranslation(key: String, translation: String) extends Command
  case class UpdateTranslation(key: String, line: Int, was: String, becomes: String) extends Command
  case object Noop extends Command

  def run(consistencyCheck: ConsistencyChecker.ConsistencyCheck): Unit = {
    val (reports, usageData) = consistencyCheck
    val nbProblems = reports.map(_.fold(_ => 1, _.nbProblems)).sum
    intro(nbProblems, reports.size)
    init(nbProblems, reports.collect{case Right(report) => report}, usageData)
  }

  def intro(nbProblems: Int, nbFiles: Int) : Unit = {
    println(s"We've detected $nbProblems translation problems across the $nbFiles translation files")
  }

  def init(nbProblems: Int, problems: Seq[FileAnalysisReport], usageData: UsageData): Unit = {
    if (nbProblems == 0) println("Perfect !")
    else if (problems.size == 1) fixFile(problems.head, usageData)
    else {
      println("Here are the problematic files")
      choice(problems.map(f => f.file.name -> f)).foreach(fixFile(_, usageData))
    }
  }

  def choice[T](items: Seq[(String, T)]): Option[T] = {
    items.zipWithIndex.foreach{ case  ((label, _), index) =>
      println(s"\t[${index+1}] $label")
    }
    println(s"Please choose one in 1-${items.size}")
    val cmd = readLine("> ")
    Try(cmd.toInt).toOption.filter(idx => idx >= 1 && idx <= items.size).map(idx => items(idx - 1)._2)
  }

  def fixFile(report : FileAnalysisReport, usageData: UsageData) : Unit = {
    val updates = report.content.map(rm => fixDefinition(report.file, rm, usageData.byKey.getOrElse(rm.message.key, Nil)))
    val additions = defineMissingKeys(report.file, report.missingKeys, usageData)

    println(s"All problems in ${green(report.file.name)} have found a solution")
    println(s"Here are the modifications we're about to perform on ${green(report.file.name)}")

    val lines = Source.fromFile(report.file).getLines().toList
    val padder = pad(padding(lines.size + additions.size))(_)

    reportUpdates(updates, padder)
    reportAdditions(additions, lines.size + 2, padder)

    val consent = readLine("Shall we proceed [y/N]\n > ")

    if (consent == "y") {
      applyResolution(report.file, lines, updates, additions)
    } else {
      println("Aborting")
    }

  }

  def reportUpdates(updates: Seq[UpdateTranslation], padder: Int => String) : Unit = {
    updates.foreach{ update =>
      println(sourceFileLine(s"${update.key} = ${update.was}", update.line, padder, Some(scala.Console.RED)))
      println(sourceFileLine(s"${update.key} = ${update.becomes}", update.line, padder, Some(scala.Console.GREEN)))
    }
  }

  def reportAdditions(additions: Seq[DefineTranslation], totalLines: Int, padder: Int => String) : Unit = {
    additions.zipWithIndex.foreach{ case (addition, i) =>
      println(sourceFileLine(s"${addition.key} = ${addition.translation}", i + totalLines, padder, Some(scala.Console.GREEN)))
    }
  }

  def applyResolution(file: File, lines: List[String],  updates: Seq[UpdateTranslation], additions: Seq[DefineTranslation]) : Unit = {
    val sortedUpdates = updates.map{
      update => (update.line - 1) -> s"${update.key} = ${update.becomes}"
    }.toMap
    val updatedLines = lines.zipWithIndex.map{case (l, i) =>
        sortedUpdates.getOrElse(i, l)
    }

    val out = new PrintWriter(new FileWriter(file, false))
    updatedLines.foreach(out.println)
    out.println()
    additions.foreach{ addition =>
      out.println(s"${addition.key} = ${addition.translation}")
    }
    out.flush()
    out.close()
  }

  def color(color: String)(s: String) = color + s + scala.Console.RESET
  val blue = color(scala.Console.BLUE)(_)
  val green = color(scala.Console.GREEN)(_)
  val red = color(scala.Console.RED)(_)

  def reportProblem(key: String, problem: ConsistencyProblem): Unit = {
    problem match {
      case UnusedTranslation =>
        println(s" * The translation ${blue(key)} is defined but never used")
      case WrongNumberOfArguments(pattern, expectedMin, actual) =>
        println(s" * The definition of ${blue(key)} has ${red(actual.toString)} parameters but it is used in the code with ${green(expectedMin.toString)} parameters")
      case PatternParseError(raw, err) =>
        println(s" * The pattern '${red(raw)}' could not be parsed. The error was : $err")
      case SuspiciousQuotesInPattern(pattern, indices) =>
        println(" * There are suspicious single quotes in the pattern")
    }
  }

  def fixDefinition(file: File, problem: RichMessage, usages: List[KeyUsage]) : UpdateTranslation = {
    println(s"We've detected ${red(problem.problems.size.toString)} problems in the definition for ${blue(problem.message.key)}")
    problem.problems.foreach(p => reportProblem(problem.message.key, p))
    reportLine(file, problem.message.pos.line)
    fix(UpdateTranslation(_, problem.message.pos.line, problem.message.pattern, _))(problem.message.key, usages)
  }

  def defineMissingKeys(file: File, missingKeys: Set[String], usageData: UsageData): Seq[DefineTranslation] = {
    missingKeys.toList.sorted.map{ key =>
      println(s"The translation ${blue(key)} is used in the code but not defined in ${green(file.name)}")
      fix(DefineTranslation)(key, usageData.byKey.getOrElse(key, Nil))
    }
  }


  def padding(max: Int) = Math.log10(max).toInt + 1

  def pad(p: Int)(l: Int):String = {
    val pattern = s" %${p}d"
    pattern.format(l)
  }

  def sourceFileLine(line: String, lineNb: Int, padder: Int => String, color: Option[String] = None): String = {
    color.getOrElse("") + padder(lineNb) + " | " + line + color.map(_ => scala.Console.RESET).getOrElse("")
  }
  def reportLine(source: File, lineNb: Int, before: Int = 0, after: Int = 0): Unit = {
    val skip = Math.max(lineNb - before - 1, 0)
    val amount = before + after + 1
    val padder = pad(padding(lineNb + after))(_)
    Source.fromFile(source).getLines.slice(skip, skip + amount).zipWithIndex.foreach{ case (line, index) =>
      val color =
        if (index == before)
          Some(scala.Console.YELLOW)
        else None
      println(sourceFileLine(line, index + skip + 1, padder, color))
    }
  }

  @tailrec
  def fix[C <: Command](cmd: (String, String) => C)(key: String, usages: List[KeyUsage]): C = {
    readTranslation(key, 0, usages) match {
      case Left(err) =>
        reportProblem(key, err)
        fix(cmd)(key, usages)
      case Right(t) =>
        if(t.problems.nonEmpty) {
          t.problems.foreach(reportProblem(key, _))
          fix(cmd)(key, usages)
        } else {
          cmd(key, t.message.pattern)
        }
    }
  }


  val session = new MessageSource {
    override def read: String = ""
  }

  def readTranslation(key: String, line: Int, usages: List[KeyUsage], originalPosition: Option[Position] = None): Either[ConsistencyProblem, RichMessage] = {
    val input = readLine(s"$key = ")
    Pattern.parse(input).right.map{ pattern =>
      val msg = Message(key, input, session, "input").atPos(originalPosition)
      val problems = ConsistencyChecker.validateMessage(msg, usages)
      RichMessage(msg, problems)
    }
  }
}
