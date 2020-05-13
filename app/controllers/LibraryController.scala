package controllers

import javax.inject._
import play.api._
import play.api.mvc._

import io.github.harwiltz.sagerank._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class LibraryController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  type Message = (Int, String)

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def showLibrary() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.library())
  }
}
