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
    val requestParams = request.queryString.map { case (k, v) => (k -> v.mkString) }
    val statusCode: Int = requestParams.get("status").map(_.toInt).getOrElse(1) // Default to "To-read" column
    val library: Iterable[ArticleMetadata] = getLibrary(State.decodeArticleStatus(statusCode))
//    val library: Iterable[ArticleMetadata] = statusCode.map(code => State.decodeArticleStatus(code))
//                                                       .map(status => getLibrary(status))
//                                                       .getOrElse(Iterable[ArticleMetadata]())
//    val idx = statusCode.map(code => code.toInt).getOrElse(1) // Default to "To Read" column
    Ok(views.html.library(statusCode, State.statusesStr, library))
  }

  def getLibrary(status: ArticleStatus): Iterable[ArticleMetadata] = State.graph.articleMap.filter { case(id, artbib) =>
    artbib.article.status match {
      case `status` => true
      case _ => false
    }
  }.map { case (k, v) => v.article }
}
