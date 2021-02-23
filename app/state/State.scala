package state

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions

import java.util.HashMap

import io.github.harwiltz.sagerank._

trait SageRankerState {
  def username: String
  var graph: SageRanker

  def persistGraph: Unit

  def withSaveGraph[A](thunk: => A): A = this.graph.synchronized {
    val retval = thunk
    this.persistGraph

    retval
  }
}

class FirestoreSageRanker(val db: Firestore, val username: String) extends SageRankerState {
  var graph = this.loadGraph

  def persistGraph: Unit = {
    val docRef = this.db.collection("users").document(this.username)
    val json = SageRankerFactory.serialize(this.graph)
    val data = new HashMap[String, Object](){ put("sageranker", json) }
    docRef.update(data)
  }

  def loadGraph: SageRanker = {
    val jsonFuture = State.fireStore.collection("users").document(username).get.get
    if(jsonFuture.exists && jsonFuture.contains("sageranker")) {
      val json = jsonFuture.get("sageranker").toString
      SageRankerFactory.fromJson(json.toString).getOrElse(new SageRanker)
    } else new SageRanker
  }
}

object State {
  val fireStore = FirestoreOptions.getDefaultInstance().toBuilder()
    .setProjectId("supervisor-trailer")
    .setCredentials(GoogleCredentials.getApplicationDefault)
    .build()
    .getService

  val statuses = Array(ReadArticle, InterestedInArticle, UnreadArticle)
  val statusesStr = statuses.map(status => libraryColumnName(status))

  def decodeArticleStatus(statusCode: Int): ArticleStatus = {
    if((statusCode >= 0) && (statusCode < statuses.size)) {
      statuses(statusCode)
    } else {
      UnreadArticle
    }
  }

  def libraryColumnName(status: ArticleStatus): String = status match {
    case ReadArticle => "Read"
    case InterestedInArticle => "To-Read"
    case UnreadArticle => "Suggestions"
    case _ => "Unknown"
  }
}
