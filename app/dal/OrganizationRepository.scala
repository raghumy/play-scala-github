package dal

import java.sql.Timestamp
import java.text.SimpleDateFormat
import javax.inject.{Inject, Singleton}

import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.Logger
import models._
import slick.dbio.DBIOAction

import scala.concurrent.{ExecutionContext, Future}

/**
  * Repository for the org
  * @param dbConfigProvider
  * @param repodb
  * @param datadb
  * @param ec
  */
@Singleton
class OrganizationRepository @Inject() (dbConfigProvider: DatabaseConfigProvider, repodb: RepoRepository, datadb: OrgDataRepository)(implicit ec: ExecutionContext) {
  // We want the JdbcProfile for this provider
  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  private val logger = Logger(getClass)

  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.
  import dbConfig._
  import profile.api._

  /**
    * Organization table
    */
  private class OrganizationTable(tag: Tag) extends Table[Organization](tag, "orgs")  {

    /** The ID column, which is the primary key, and auto incremented */
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name")

    def last_updated = column[Timestamp]("last_updated", O.Default(new Timestamp(0L)))

    def state = column[Option[String]]("state")

    /**
      * This is the tables default "projection".
      */
    def * = (id, name, last_updated, state) <> ((Organization.apply _).tupled, Organization.unapply)
  }

  /**
    * The starting point for all queries on the organization table.
    */
  private val organization = TableQuery[OrganizationTable]

  def insert(o: Organization) =
    organization returning organization.map(_.id) += o

  /**
    * Create a new org
    * @param org
    * @return
    */
  def create(org: String) = insert(Organization(0, org, new Timestamp(new java.util.Date().getTime()), None))

  /**
    * List all the organizations in the database.
    */
  def list(): Future[Seq[Organization]] = db.run {
    organization.result
  }

  private def _findByName(org: String): DBIO[Option[Organization]] =
    organization.filter(_.name === org).result.headOption

  def needUpdate(t: Timestamp): Future[List[Organization]] =
    db.run(organization.filter(_.state.isEmpty).filter(_.last_updated < t).to[List].result)

  /**
    * Find an org
    * @param org
    * @return
    */
  def findOrg(org: String): Future[Option[Organization]] = db.run(_findByName(org))

  /**
    * Get JSON data for this org
    * @param org
    * @return
    */
  def getOrgData(org: String) = datadb.findByOrg(org)

  /**
    * Helper function to add both the org and it's data
    * @param org
    * @return
    */
  def addOrg(org: String) = Future {
    db.run(create(org) andThen
      datadb.create(org))
  }

  /**
    * Update the last time the stats where updated
    * @param org
    * @return
    */
  def updateLastUpdated(org: String) = db.run {
    organization.filter(_.name === org).map(_.last_updated).update(new Timestamp(new java.util.Date().getTime()))
  }

  def updateState(org: String, s: Option[String]) = db.run {
    organization.filter(_.name === org).map(_.state).update(s)
  }

  def updateStateIfEmpty(org: String, s: Option[String]) = db.run {
    organization.filter(_.name === org).filter(_.state.isEmpty).map(_.state).update(s)
  }

  /**
    * Helper function to update stats for this org.
    * @param org
    * @param json
    * @return
    */
  def updateStats(org: String, json: JsValue) = Future {
    // Need to figure out if we can do this once
    implicit object timestampFormat extends Format[Timestamp] {
      val format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
      def reads(json: JsValue) = {
        val str = json.as[String]
        JsSuccess(new Timestamp(format.parse(str).getTime))
      }
      def writes(ts: Timestamp) = JsString(format.format(ts))
    }


    implicit val repoReads = (
        (JsPath \\ "name").read[String] and
          (JsPath \\ "forks_count").read[Int] and
          (JsPath \\ "updated_at").read[Timestamp] and
          (JsPath \\ "open_issues_count").read[Int] and
            (JsPath \\ "stargazers_count").read[Int] and
            (JsPath \\ "watchers_count").read[Int]
      )(Repo.apply(0L,_,org,_,_,_,_,_))

    val repos:List[Repo] = json.as[List[models.Repo]]

    logger.trace(s"json parsed ${repos.size}")

    db.run(repodb._deleteAllInOrg(org) andThen
      repodb.bulkInsert(repos)
    )

    updateLastUpdated(org)

    logger.trace("All rows inserted")
  }

  // List of functions to get stats

  def getStats(org: String) = repodb.findByOrg(org)

  def getStatsByForks(org: String, n: Int) = repodb.statsByForks(org, n)

  def getStatsByLastUpdated(org: String, n: Int) = repodb.statsByLastUpdated(org, n)

  def getStatsByOpenIssues(org: String, n: Int) = repodb.statsByOpenIssues(org, n)

  def getStatsByStars(org: String, n: Int) = repodb.statsByStars(org, n)

  def getStatsByWatchers(org: String, n: Int) = repodb.statsByWatchers(org, n)
}
