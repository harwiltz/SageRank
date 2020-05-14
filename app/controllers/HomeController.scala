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

  type Message = (Int, String)

  val graphPath = "graph.json"

  var graph = SageRankerFactory.fromFile(graphPath)
                               .getOrElse(new SageRanker)

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
    val articleMessage = if(requestParams.get("url").getOrElse("").isEmpty) {
                            None
                          } else article match {
                            case None => Some((-1, "Sorry, SageRank could not understand the given URL."))
                            case Some(artbib) => Some((1, s"Added '${artbib.article.title}' and its citations to your library!"))
                          }
    article foreach { a => withSaveGraph(graphPath) { graph = graph.withArticleGraph(a) } }

    val graphMessage = graph.articleMap.size match {
      case 0 => Some(-1, "Your graph is empty. Try adding an article!")
      case n => Some(0, s"${n} articles in the graph")
    }
    val messages = Array(graphMessage, articleMessage).flatten
    Ok(views.html.index(messages, graph.articleMap.size != 0))
  }

  def withSaveGraph(path: String)(thunk: => Unit): Unit = graph.synchronized {
    thunk
    SageRankerFactory.save(graph, graphPath)
  }
}
