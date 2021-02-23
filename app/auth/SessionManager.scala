package auth

import java.time.LocalDateTime
import java.util.UUID

import javax.inject._
import play.api._
import play.api.mvc._

import scala.collection.mutable

import scala.concurrent.{ExecutionContext, Future}

import state._

case class Session(token: String, user: SageRankerState, expiration: LocalDateTime)

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
    val sageRanker = new FirestoreSageRanker(State.fireStore, username)
    sessions.synchronized {
      sessions.put(token, Session(token, sageRanker, LocalDateTime.now().plusHours(24)))
    }

    token
  }

  def getSessionUser(request: RequestHeader): Option[SageRankerState] = {
    request.session.get("sessionToken")
      .flatMap(sessions.get(_))
      .map(_.user)
  }
}

class UserRequest[A](val user: Option[SageRankerState], val request: Request[A])
    extends WrappedRequest[A](request)

class UserAction @Inject()(val parser: BodyParsers.Default)(implicit val executionContext: ExecutionContext)
    extends ActionBuilder[UserRequest, AnyContent]
    with ActionTransformer[Request, UserRequest] {
  def transform[A](request: Request[A]) = Future.successful {
    new UserRequest(SessionManager.getSessionUser(request), request)
  }
}
