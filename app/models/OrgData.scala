package models

import play.api.libs.json._

/**
  * Class that stores json data for the organization
  * @param id
  * @param org
  * @param members_json
  * @param repos_json
  */
case class OrgData(id: Long, org: String, members_json: Option[String] = None, repos_json: Option[String] = None)

object OrgData {

  implicit val orgDataFormat = Json.format[OrgData]
}