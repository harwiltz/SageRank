package controllers

import io.github.harwiltz.sagerank._

object State {
  val graphPath = "graph.json"

  var graph = SageRankerFactory.fromFile(graphPath)
                                    .getOrElse(new SageRanker)

  val statuses = Array(ReadArticle, InterestedInArticle, UnreadArticle)
  val statusesStr = statuses.map(status => libraryColumnName(status))

  def withSaveGraph(path: String)(thunk: => Unit): Unit = graph.synchronized {
    thunk
    SageRankerFactory.save(graph, graphPath)
  }

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
