package io.kanaka.e2e.demo

import io.kanaka.e2e.MessagesBackend

import scala.reflect.macros.blackbox
import scala.language.experimental.macros

/**
  * @author Valentin Kasas
  */
object Messages extends MessagesBackend {
  override def materializeCall(c: blackbox.Context)(parameter: c.Expr[String]): c.Tree = {
    import c.universe._

    q""""Label for " + $parameter"""
  }

  def apply(key: String): String = macro impl

}
