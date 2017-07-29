package models

import play.api.libs.json._

case class Repo(id: Long, name: String, org: String, forks: Int, last_updated: Long, open_issues: Int, stars: Int, watchers: Int)

object Repo {

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