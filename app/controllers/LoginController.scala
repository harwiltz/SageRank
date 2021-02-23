package controllers

import javax.inject._
import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.mvc._

import io.github.harwiltz.sagerank._

case class UserCredentials(email: String, password: String)

@Singleton
class LoginController @Inject()(messagesAction: MessagesActionBuilder,
  val controllerComponents: ControllerComponents) extends BaseController {
  val form = Form(
    mapping(
      "email" -> email,
      "password" -> nonEmptyText(minLength = 8)
    )(UserCredentials.apply)(UserCredentials.unapply)
  )

  def index() = messagesAction { implicit request: MessagesRequest[AnyContent] =>
    Ok(views.html.login(form))
  }

  def login() = messagesAction { implicit request: MessagesRequest[AnyContent] =>
    form.bindFromRequest.fold(
      err => {
        BadRequest(views.html.login(err))
      },
      credentials => {
        Ok(s"User: ${credentials.email}, Password: ${credentials.password}")
      }
    )
  }

  def newUser = TODO
}
