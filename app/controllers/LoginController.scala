package controllers

import java.util.HashMap
import javax.inject._
import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.mvc._

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions

import io.github.harwiltz.sagerank._

import auth.SessionManager

case class UserCredentials(username: String, password: String)
case class RegisterUserForm(username: String, password: String, retryPassword: String)

object PasswordManager {
  import java.security.SecureRandom
  import java.util.Base64
  import javax.crypto.spec.PBEKeySpec
  import javax.crypto.SecretKeyFactory

  val defaultIterations = 10000
  val random = new SecureRandom

  def pbkdf2(password: String, salt: Array[Byte], iterations: Int): Array[Byte] = {
    val keySpec = new PBEKeySpec(password.toCharArray, salt, iterations, 256)
    val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    keyFactory.generateSecret(keySpec).getEncoded
  }

  def hash(password: String): String = {
    val salt = new Array[Byte](16)
    random.nextBytes(salt)
    val hash = pbkdf2(password, salt, defaultIterations)
    val saltStr = Base64.getEncoder.encodeToString(salt)
    val hashStr = Base64.getEncoder.encodeToString(hash)
    s"${defaultIterations}:${saltStr}:${hashStr}"
  }

  def check(password: String, pwdHash: String) = {
    pwdHash.split(":") match {
      case Array(iterations, saltStr, hashStr) if iterations.forall(_.isDigit) => {
        val hashDcd = Base64.getDecoder.decode(hashStr)
        val saltDcd = Base64.getDecoder.decode(saltStr)
        val passwordHash = pbkdf2(password, saltDcd, iterations.toInt)
        passwordHash.sameElements(hashDcd)
      }
      case other => false
    }
  }
}

@Singleton
class LoginController @Inject()(messagesAction: MessagesActionBuilder,
  val controllerComponents: ControllerComponents) extends BaseController {
  val form = Form(
    mapping(
      "username" -> nonEmptyText(minLength = 4),
      "password" -> nonEmptyText(minLength = 8)
    )(UserCredentials.apply)(UserCredentials.unapply)
  )

  val registerForm = Form(
    mapping(
      "username" -> nonEmptyText(minLength = 4),
      "password" -> nonEmptyText(minLength = 8),
      "retryPassword" -> nonEmptyText(minLength = 8)
    )(RegisterUserForm.apply)(RegisterUserForm.unapply)
  )

  lazy val firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
    .setProjectId("supervisor-trailer")
    .setCredentials(GoogleCredentials.getApplicationDefault)
    .build

  lazy val db = firestoreOptions.getService

  def index() = messagesAction { implicit request: MessagesRequest[AnyContent] =>
    Ok(views.html.login(form))
  }

  def login() = messagesAction { implicit request: MessagesRequest[AnyContent] =>
    form.bindFromRequest.fold(
      err => {
        BadRequest(views.html.login(err))
      },
      credentials => {
        val docRef = db.collection("users").document(credentials.username).get
        docRef.get.exists match {
          case true => {
            val passwordHash = docRef.get.get("password")
            if(PasswordManager.check(credentials.password, passwordHash.toString)) {
              val token = SessionManager.newSession(credentials.username)
              Redirect(routes.HomeController.index())
                .withSession(request.session + ("sessionToken" -> token))
            } else {
              Unauthorized(views.html.defaultpages.unauthorized()).withNewSession
            }
          }
          case false => BadRequest("Invalid username")
        }
      }
    )
  }

  def logout() = Action {
    Redirect("/login").withNewSession
  }

  def newUser = messagesAction { implicit request: MessagesRequest[AnyContent] =>
    Ok(views.html.register(registerForm))
  }

  def register = messagesAction { implicit request: MessagesRequest[AnyContent] =>
    registerForm.bindFromRequest.fold(
      err => {
        BadRequest(views.html.register(err))
      },
      credentials => {
        val docRef = db.collection("users").document(credentials.username)
        docRef.get.get.exists match {
          case true => {
            BadRequest(views.html.register(registerForm, List("Username is taken")))
          }
          case false => {
            if(credentials.password.equals(credentials.retryPassword)) {
              val passwordHash = PasswordManager.hash(credentials.password)
              val data = new HashMap[String, Object](){ put("password", passwordHash) }
              docRef.set(data)
              Redirect("/login").withNewSession
            } else {
              BadRequest(views.html.register(registerForm, List("Passwords don't match")))
            }
          }
        }
      }
    )
  }
}
