package io.kanaka.e2e

import java.io.{File, FileWriter}

import scala.reflect.macros.blackbox

case class OutputConfiguration(applicationId: Long, out: FileWriter)

trait MessagesBackend { self: Singleton =>

  def materializeCall(c: blackbox.Context)(parameter: c.Expr[String]): c.Tree

  def impl(c: blackbox.Context)(key: c.Expr[String]): c.Tree = {
    extractValue(c)(key).foreach(recordUsage(c))
    materializeCall(c)(key)
  }

  def extractValue(c: blackbox.Context)(expr: c.Expr[String]): Option[String] = {
    import c.universe._

    expr match {
      case Expr(Literal(Constant(str: String))) => Some(str)
      case _ =>
        c.warning(expr.tree.pos, "Non-literal message keys are not managed by e2e.")
        None
    }
  }

  def configureOutput(c: blackbox.Context): OutputConfiguration = {
    import c.universe._
    import internal._

    val sym = c.macroApplication.symbol
    attachments(sym).get[OutputConfiguration].getOrElse{
      val outputPath = c.settings.head
      val outDir= new File(outputPath)
      outDir.mkdirs()
      val outFile = new File(outDir, "key_usages")
      val writer = new FileWriter(outFile, true)
      val config = OutputConfiguration(System.currentTimeMillis(), writer)
      updateAttachment(sym, config)
      config
    }
  }

  def recordUsage(c: blackbox.Context)(key: String): Unit = {
    val pos = c.macroApplication.pos
    val OutputConfiguration(applicationId, out) = configureOutput(c)
    out.write(s"$applicationId;${pos.source.path};$key;${pos.line};\n")
    out.flush()
  }
}




