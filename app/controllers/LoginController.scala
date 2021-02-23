package controllers

import javax.inject._
import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.mvc._

import io.github.harwiltz.sagerank._

case class UserCredentials(email: String, password: String)

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
    keyfactory.generateSecret(keySpec).getEncoded
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
    pwdHash.split(":").match {
      case Array(iterations, saltStr, hashStr) if iterations.forall(_.isDigit) => {
        val hashDcd = Base64.getDecoder.decode(hashStr)
        val saltDcd = Base64.getDecoder.decode(saltStr)
        val passwordHash = pbkfd2(password, saltDcd, iterations)
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
      "email" -> email,
      "password" -> nonEmptyText(minLength = 8)
    )(UserCredentials.apply)(UserCredentials.unapply)
  )

  def index() = messagesAction { implicit request: MessagesRequest[AnyContent] =>
    Ok(views.html.login(form))
  }

  def login() = messagesAction { implicit request: MessagesRequest[AnyContent] =>
    form.bindFromRequest.fold(
      err => {
        BadRequest(views.html.login(err))
      },
      credentials => {
        Ok(s"User: ${credentials.email}, Password: ${credentials.password}")
      }
    )
  }

  def newUser = TODO
}
