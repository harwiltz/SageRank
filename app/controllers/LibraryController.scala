package controllers

import javax.inject._
import play.api._
import play.api.mvc._

import io.github.harwiltz.sagerank._

import auth.UserRequest
import auth.UserAction
import state._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class LibraryController @Inject()(val controllerComponents: ControllerComponents, val userAction: UserAction) extends BaseController {

  val libraryStatusColumns: Array[String] = State.statusesStr.dropRight(1)

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def showLibrary() = userAction { implicit userRequest: UserRequest[AnyContent] =>
    userRequest.user.map { user =>
      val requestParams = userRequest.request.queryString.map { case (k, v) => (k -> v.mkString) }
      val statusCode: Int = requestParams.get("status").map(_.toInt).getOrElse(1) // Default to "To-read" column
      val library: Iterable[ArticleMetadata] = getLibrary(user, State.decodeArticleStatus(statusCode))
      Ok(views.html.library(user.username, statusCode, libraryStatusColumns, library))
    }.getOrElse(Redirect("/login").withNewSession)
  }

  def updateStatus() = userAction { implicit userRequest: UserRequest[AnyContent] =>
    userRequest.user.map { user =>
      val requestParams = userRequest.request.queryString.map { case (k, v) => (k -> v.mkString) }
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
          val title = user.graph.articleMap.get(a.article.id).map(x => x.article.title)
          val forceChange = statusDeltaCode > 0
          user.withSaveGraph {
            user.graph = user.graph.withChangedStatus(status, forceChange)(a)
          }
          title match {
            case None => (-1, "Couldn't find requested article in library.")
            case Some(t) => (1, s"Expanded citation tree for '${t}'.")
          }
        }
      }
      val library: Iterable[ArticleMetadata] = getLibrary(user, State.decodeArticleStatus(statusCode))
      Ok(views.html.library(user.username, statusCode, libraryStatusColumns, library))
    }.getOrElse(Redirect("/login").withNewSession)
  }

  def getLibrary(sageRanker: SageRankerState, status: ArticleStatus): Iterable[ArticleMetadata] = {
    sageRanker.graph.articleMap.filter { case(id, artbib) =>
      artbib.article.status match {
        case `status` => true
        case _ => false
      }
    }.map { case (k, v) => v.article }
  }
}
