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

  val libraryStatusColumns: Array[String] = State.statusesStr.dropRight(1)

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
    Ok(views.html.library(statusCode, libraryStatusColumns, library))
  }

  def updateStatus() = Action { implicit request: Request[AnyContent] =>
    val requestParams = request.queryString.map { case (k, v) => (k -> v.mkString) }
    val statusCode: Int = requestParams.get("status").map(_.toInt).getOrElse(1) // Default to "To-read" column
    val statusDeltaCode: Int = requestParams.get("statusDelta").map(_.toInt).getOrElse(0)
    val articleId: Option[String] = requestParams.get("articleid")
    val articleMetadata: Option[ArticleMetadata] = articleId.map { id =>
      ArticleMetadata(id,
                      "",
                      Vector[String](),
                      "",
                      "",
                      "",
                      UnreadArticle)
    }
    val artbib = articleMetadata.map(am => ArticleBibliography(am, Vector[ArticleBibliography]()))

    val articleMessage = requestParams.get("status").flatMap { statusCode =>
      val status = State.decodeArticleStatus(statusCode.toInt + statusDeltaCode)
      artbib.map { a =>
        val title = State.graph.articleMap.get(a.article.id).map(x => x.article.title)
        val forceChange = statusDeltaCode > 0
        State.withSaveGraph(State.graphPath) {
          State.graph = State.graph.withChangedStatus(status, forceChange)(a)
        }
        title match {
          case None => (-1, "Couldn't find requested article in library.")
          case Some(t) => (1, s"Expanded citation tree for '${t}'.")
        }
      }
    }
    val library: Iterable[ArticleMetadata] = getLibrary(State.decodeArticleStatus(statusCode))
    Ok(views.html.library(statusCode, libraryStatusColumns, library))
  }

  def getLibrary(status: ArticleStatus): Iterable[ArticleMetadata] = State.graph.articleMap.filter { case(id, artbib) =>
    artbib.article.status match {
      case `status` => true
      case _ => false
    }
  }.map { case (k, v) => v.article }
}
