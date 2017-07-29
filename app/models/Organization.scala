package models

import play.api.libs.json._

case class Organization(id: Long, name: String)

object Organization {

  implicit val organizationFormat = Json.format[Organization]
}