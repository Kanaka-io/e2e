package controllers

import play.api._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import io.kanaka.e2e.play.Messages

class Application(val messagesApi: MessagesApi) extends Controller with I18nSupport {

  def index = Action {
    Ok(views.html.index(Messages("user.greet")))
  }

  def missing = Action {
    Ok(views.html.index(Messages("missing.key")))
  }

}
