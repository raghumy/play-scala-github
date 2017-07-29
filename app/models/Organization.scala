package models

import play.api.libs.json._

case class Organization(id: Long, name: String, last_updated: Long, state: Option[String] = None)

object Organization {

  implicit val organizationFormat = Json.format[Organization]
}