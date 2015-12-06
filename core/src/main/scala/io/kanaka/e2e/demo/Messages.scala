package io.kanaka.e2e.demo

import io.kanaka.e2e.MessagesBackend

import scala.reflect.macros.blackbox
import scala.language.experimental.macros

/**
  * @author Valentin Kasas
  */
object Messages extends MessagesBackend {
  override def materializeCall(c: blackbox.Context)(key: c.Expr[String], parameters: c.Expr[Any]*): c.Tree = {
    import c.universe._

    q""""Label for " + $key """
  }

  def apply(key: String, parameters: Any*): String = macro impl

}
