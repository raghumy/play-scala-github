package models

import play.api.libs.json._

case class OrgData(id: Long, org: String, members_json: Option[String] = None, repos_json: Option[String] = None)

object OrgData {

  implicit val orgDataFormat = Json.format[OrgData]
}