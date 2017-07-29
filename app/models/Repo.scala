package models

import play.api.libs.json._
import java.sql.Timestamp
import java.text.SimpleDateFormat


case class Repo(id: Long, name: String, org: String, forks: Int, last_updated: Timestamp, open_issues: Int, stars: Int, watchers: Int)

object Repo {

  implicit object timestampFormat extends Format[Timestamp] {
    val format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    def reads(json: JsValue) = {
      val str = json.as[String]
      JsSuccess(new Timestamp(format.parse(str).getTime))
    }
    def writes(ts: Timestamp) = JsString(format.format(ts))
  }

  implicit val repoFormat = Json.format[Repo]

  /*
  implicit val repoReads: Reads[Repo] = (
    (JsPath \\ "name").read[String] and
      (JsPath \ "forks").read[Int] and
      (JsPath \ "forks").read[Int] and
      (JsPath \ "forks").read[Int]
    ) (Repo.apply _)
    */
}