package controllers

import javax.inject.Inject

import io.kanaka.e2e.play.Messages
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

class Application @Inject() (val messagesApi: MessagesApi) extends Controller with I18nSupport {

  def index = Action {
    Ok(views.html.index(Messages("user.greet", "World")))
  }

  def missing = Action {
    Ok(views.html.index(Messages("missing.key")))
  }

}
