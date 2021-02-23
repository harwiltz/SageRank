package auth

import java.time.LocalDateTime
import java.util.UUID

import javax.inject._
import play.api._
import play.api.mvc._

import scala.collection.mutable

import scala.concurrent.{ExecutionContext, Future}

case class Session(token: String, username: String, expiration: LocalDateTime)

object SessionManager {
  private val sessions = mutable.Map.empty[String, Session]

  def getSession(token: String): Option[Session] = {
    sessions.get(token).flatMap { t =>
      if(LocalDateTime.now().isAfter(t.expiration)) {
        sessions.remove(t.token)
        None
      } else {
        Some(t)
      }
    }
  }

  def newSession(username: String): String = {
    val token = s"$username-token-${UUID.randomUUID().toString}"
    sessions.synchronized {
      sessions.put(token, Session(token, username, LocalDateTime.now().plusHours(24)))
    }

    token
  }

  def getSessionUser(request: RequestHeader): Option[String] = {
    request.session.get("sessionToken")
      .flatMap(sessions.get(_))
      .map(_.username)
  }
}

class UserRequest[A](val username: Option[String], val request: Request[A])
    extends WrappedRequest[A](request)

class UserAction @Inject()(val parser: BodyParsers.Default)(implicit val executionContext: ExecutionContext)
    extends ActionBuilder[UserRequest, AnyContent]
    with ActionTransformer[Request, UserRequest] {
  def transform[A](request: Request[A]) = Future.successful {
    new UserRequest(SessionManager.getSessionUser(request), request)
  }
}
