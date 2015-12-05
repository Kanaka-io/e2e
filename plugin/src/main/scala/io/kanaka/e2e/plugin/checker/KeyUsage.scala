package io.kanaka.e2e.plugin.checker

import scala.util.Try

/**
  * @author Valentin Kasas
  */
case class KeyUsage(applicationId: Long, path: String, key: String, line: Int) {
  def toCSV:String = productIterator.mkString(";")
}

object KeyUsage {
  def fromCSV(record: String): Option[KeyUsage] = {
    val fields = record.split(";")
    fields match {
      case
        Array(a, p, k, l) =>
        Try(KeyUsage(a.toLong, p, k, l.toInt)).toOption
      case _ => None
    }
  }
}
