package io.kanaka.e2e.play

import io.kanaka.e2e.MessagesBackend

import scala.reflect.macros.blackbox
import scala.language.experimental.macros


/**
  * @author Valentin Kasas
  */
object Messages extends MessagesBackend {
  override def materializeCall(c: blackbox.Context)(parameter: c.Expr[String]): c.Tree = {
    import c.universe._

    q"""play.api.i18n.Messages($parameter)"""
  }

  def apply(key: String): String = macro impl

}
