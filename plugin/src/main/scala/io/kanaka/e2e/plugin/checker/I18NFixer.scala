package io.kanaka.e2e.plugin.checker

import java.io
import java.io.{PrintWriter, FileWriter}

import sbt._

import scala.io.Source
import scala.util.Try

/**
  * @author Valentin Kasas
  */

case class Position(file: File, line: Int)


object I18NFixer {

  sealed trait Command
  case class DefineTranslation(key: String, translation: String) extends Command
  case class UpdateTranslation(key: String, line: Int, was: String, becomes: String) extends Command

  def run(problems: Map[File, Set[TranslationProblem]]): Unit = {
    val nbProblems =   problems.mapValues(_.size).values.sum
    intro(nbProblems, problems.size)
    init(nbProblems, problems)
  }

  def intro(nbProblems: Int, nbFiles: Int) : Unit = {
    println(s"We've detected $nbProblems translation problems across the $nbFiles translation files")
  }

  def init(nbProblems: Int, problems: Map[File, Set[TranslationProblem]]): Unit = {
    if (nbProblems == 0) println("Perfect !")
    else if (problems.size == 1) fixFile(problems.head._1, problems.head._2)
    else {
      println("Here are the problematic files")
      choice(problems.keys.map(f => f.name -> f).toSeq).foreach{ file =>
        fixFile(file, problems(file))
      }
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

  def fixFile(file: File, problems: Set[TranslationProblem]) : Unit = {
    val resolutions = problems.map(fixProblem(file, _))
    val updates = resolutions.collect{ case u: UpdateTranslation => u}.toList.sortBy(_.line)
    val additions = resolutions.collect{ case d: DefineTranslation => d}.toList.sortBy(_.key)

    println(s"All problems in ${scala.Console.GREEN}${file.name}${scala.Console.RESET} have found a solution")
    println(s"Here are the modifications we're about to perform on ${file.name}")

    val lines = Source.fromFile(file).getLines().toList
    val padder = pad(padding(lines.size + additions.size))(_)

    reportUpdates(updates, padder)
    reportAdditions(additions, lines.size + 2, padder)

    val consent = readLine("Shall we proceed [y/N]\n > ")

    if (consent == "y") {
      applyResolution(file, lines, updates, additions)
    } else {
      println("Aborting")
    }

  }

  def reportUpdates(updates: List[UpdateTranslation], padder: Int => String) : Unit = {
    updates.foreach{ update =>
      println(sourceFileLine(s"${update.key} = ${update.was}", update.line, padder, Some(scala.Console.RED)))
      println(sourceFileLine(s"${update.key} = ${update.becomes}", update.line, padder, Some(scala.Console.GREEN)))
    }
  }

  def reportAdditions(additions: List[DefineTranslation], totalLines: Int, padder: Int => String) : Unit = {
    additions.zipWithIndex.foreach{ case (addition, i) =>
      println(sourceFileLine(s"${addition.key} = ${addition.translation}", i + totalLines, padder, Some(scala.Console.GREEN)))
    }
  }

  def applyResolution(file: File, lines: List[String],  updates: List[UpdateTranslation], additions: List[DefineTranslation]) : Unit = {
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

  def fixProblem(file: File, problem: TranslationProblem) : Command = problem match {
    case MissingTranslation(key) =>
      defineKey(key)
    case WrongNumberOfArguments(key, pattern, expectedMin, actual, line) =>
      println(s"The definition of $key has $actual parameters but it is used in the code with $expectedMin parameters")
      reportLine(file, line)
      modifyKey(key, line, pattern)
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

  def defineKey(key: String): Command = {
    val translation = readLine(s"$key = ")
    DefineTranslation(key, translation)
  }

  def modifyKey(key: String, line: Int, was: String): Command = {
    val newTranslation = readLine(s"$key = ")
    UpdateTranslation(key, line, was, newTranslation)
  }
}
