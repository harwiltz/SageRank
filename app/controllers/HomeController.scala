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
class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  var graph = new SageRanker

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    val requestParams = request.queryString.map { case (k, v) => (k -> v.mkString) }
    val article = requestParams.get("url").flatMap(url => Article.fromURL(url, getReferences = true))
    article foreach { a => graph = graph.withArticleGraph(a) }
    Ok(views.html.index(graph.articleMap.size))
  }
}
