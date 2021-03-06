package dal

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.Logger

import models._

import scala.concurrent.{ Future, ExecutionContext }

/**
  * Class that holds JSON data for the org
  * @param dbConfigProvider
  * @param ec
  */
@Singleton
class OrgDataRepository @Inject() (dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  // We want the JdbcProfile for this provider
  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  private val logger = Logger(getClass)

  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.
  import dbConfig._
  import profile.api._

  /**
    * Table definition
    */
  private class OrgDataTable(tag: Tag) extends Table[OrgData](tag, "orgs_data")  {

    /** The ID column, which is the primary key, and auto incremented */
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def org = column[String]("org")

    def members_json = column[Option[String]]("members_json", O.SqlType("CLOB"))

    def repos_json = column[Option[String]]("repos_json", O.SqlType("CLOB"))

    /**
      * This is the tables default "projection".
      */
    def * = (id, org, members_json, repos_json) <> ((OrgData.apply _).tupled, OrgData.unapply)
  }

  /**
    * The starting point for all queries on the OrgDataTable table.
    */
  private val orgdata = TableQuery[OrgDataTable]

  def insert(o: OrgData) =
    orgdata returning orgdata.map(_.id) += o

  def create(org: String) = {
    logger.trace("Creating org_data row")
    insert(OrgData(0, org, Some("temp"), Some("temp")))
  }

  def updateMembersJson(org: String, json: JsValue) = db.run {
    orgdata.filter(_.org === org).map(_.members_json).update(Some(json.toString()))
  }

  def updateReposJson(org: String, json: JsValue) = db.run {
    orgdata.filter(_.org === org).map(_.repos_json).update(Some(json.toString()))
  }

  private def _findByName(org: String): DBIO[Option[OrgData]] =
    orgdata.filter(_.org === org).result.headOption

  def findByOrg(org: String): Future[Option[OrgData]] = db.run(_findByName(org))
}
