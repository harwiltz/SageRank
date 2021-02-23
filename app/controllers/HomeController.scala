package controllers

import javax.inject._
import play.api._
import play.api.mvc._

import io.github.harwiltz.sagerank._

import auth.SessionManager
import auth.UserRequest
import auth.UserAction

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents, val userAction: UserAction)
    extends BaseController {

  type Message = (Int, String)

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = userAction { implicit userRequest: UserRequest[AnyContent] =>
    userRequest.username.map { username =>
      val requestParams = userRequest.request.queryString.map { case (k, v) => (k -> v.mkString) }
      val article = requestParams.get("url").flatMap(url => Article.fromURL(url, getReferences = true))
      val articleMessage = if(requestParams.get("url").getOrElse("").isEmpty) {
        None
      } else article match {
        case None => Some((-1, "Sorry, SageRank could not understand the given URL."))
        case Some(artbib) => Some((1, s"Added '${artbib.article.title}' and its citations to your library!"))
      }
      article foreach { a => State.withSaveGraph(State.graphPath) { State.graph = State.graph.withArticleGraph(a) } }

      val graphMessage = State.graph.articleMap.size match {
        case 0 => Some(-1, "Your graph is empty. Try adding an article!")
        case n => Some(0, s"${n} articles in the graph")
      }
      val messages = Array(graphMessage, articleMessage).flatten
      Ok(views.html.index(username, messages, State.graph.articleMap.size != 0))
    }.getOrElse(Redirect("/login").withNewSession)
  }

  def suggestion() = userAction { implicit userRequest: UserRequest[AnyContent] =>
    userRequest.username.map { username =>
      val requestParams = userRequest.request.queryString.map { case (k, v) => (k -> v.mkString) }

      val graphMessage = State.graph.articleMap.size match {
        case 0 => Some(-1, "Your graph is empty. Try adding an article!")
        case n => Some(0, s"${n} articles in the graph")
      }
      val messages = Array(graphMessage).flatten
      val suggestedArticle = if(State.graph.articleMap.isEmpty) {
        None
      } else {
        val article = State.graph.suggestUnread
        Some(Article.attachAbstract(article).article)
      }
      Ok(views.html.index(username, messages, State.graph.articleMap.size != 0, suggestedArticle))
    }.getOrElse(Redirect("/login").withNewSession)
  }

  def updateStatus() = userAction { implicit userRequest: UserRequest[AnyContent] =>
    userRequest.username.map { username =>
      val requestParams = userRequest.request.queryString.map { case (k, v) => (k -> v.mkString) }

      val articleId = requestParams.get("articleid")
      val articleTitle = requestParams.get("title").getOrElse("Unknown Title")
      val articleMetadata = articleId.map { id =>
        ArticleMetadata(id,
          articleTitle,
          Vector[String](),
          "",
          "",
          "",
          UnreadArticle)
      }
      val artbib = articleMetadata.map(am => ArticleBibliography(am, Vector[ArticleBibliography]()))

      val articleMessage = requestParams.get("status").flatMap { statusCode =>
        val status = State.decodeArticleStatus(statusCode.toInt)
        artbib.map { a =>
          val title = State.graph.articleMap.get(a.article.id).map(x => x.article.title)
          State.withSaveGraph(State.graphPath) { State.graph = State.graph.withChangedStatus(status)(a) }
          title match {
            case None => (-1, "Couldn't find requested article in library.")
            case Some(t) => (1, s"Expanded citation tree for '${t}'.")
          }
        }
      }

      val graphMessage = State.graph.articleMap.size match {
        case 0 => Some(-1, "Your graph is empty. Try adding an article!")
        case n => Some(0, s"${n} articles in the graph")
      }
      val messages = Array(graphMessage, articleMessage).flatten
      val suggestedArticle = if(State.graph.articleMap.isEmpty) {
        None
      } else {
        val article = State.graph.suggestUnread
        Some(Article.attachAbstract(article).article)
      }
      Ok(views.html.index(username, messages, State.graph.articleMap.size != 0, suggestedArticle))
    }.getOrElse(Redirect("/login").withNewSession)
  }
}
