package models

import java.sql.Timestamp
import java.text.SimpleDateFormat

import play.api.libs.json._

/**
  * Models an Organization
  * @param id
  * @param name
  * @param last_updated
  * @param state
  */
case class Organization(id: Long, name: String, last_updated: Timestamp, state: Option[String] = None)

object Organization {

  /**
    * Object used for timestamp conversion
    */
  implicit object timestampFormat extends Format[Timestamp] {
    val format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    def reads(json: JsValue) = {
      val str = json.as[String]
      JsSuccess(new Timestamp(format.parse(str).getTime))
    }
    def writes(ts: Timestamp) = JsString(format.format(ts))
  }

  implicit val organizationFormat = Json.format[Organization]
}