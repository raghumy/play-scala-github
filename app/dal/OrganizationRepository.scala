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
  * A repository for people.
  *
  * @param dbConfigProvider The Play db config provider. Play will inject this for you.
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
    * Here we define the table. It will have a name of organization
    */
  private class OrganizationTable(tag: Tag) extends Table[Organization](tag, "orgs")  {

    /** The ID column, which is the primary key, and auto incremented */
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    /** The name column is the primary key*/
    def name = column[String]("name")

    def last_updated = column[Long]("last_updated", O.Default(0))

    def state = column[Option[String]]("state")

    /**
      * This is the tables default "projection".
      *
      * It defines how the columns are converted to and from the Person object.
      *
      * In this case, we are simply passing the name parameters to the Organization case classes
      * apply and unapply methods.
      */
    def * = (id, name, last_updated, state) <> ((Organization.apply _).tupled, Organization.unapply)
  }

  /**
    * The starting point for all queries on the organization table.
    */
  private val organization = TableQuery[OrganizationTable]

  def insert(o: Organization) =
    organization returning organization.map(_.id) += o

  def create(org: String) = insert(Organization(0, org, 0, None))

  /**
    * List all the organizations in the database.
    */
  def list(): Future[Seq[Organization]] = db.run {
    organization.result
  }

  private def _findByName(org: String): DBIO[Option[Organization]] =
    organization.filter(_.name === org).result.headOption

  def findOrg(org: String): Future[Option[Organization]] = db.run(_findByName(org))

  def getOrgData(org: String) = datadb.findByOrg(org)

  def addOrg(org: String) = Future {
    db.run(create(org) andThen
      datadb.create(org))
  }

  def updateStats(org: String, json: JsValue) = Future {
    implicit val repoReads = (
        (JsPath \\ "name").read[String] and
          (JsPath \\ "forks_count").read[Int] and
          // Skip last_updated
          (JsPath \\ "open_issues_count").read[Int] and
            (JsPath \\ "stargazers_count").read[Int] and
            (JsPath \\ "watchers_count").read[Int]
      )(Repo.apply(0L,_,org,_,0L,_,_,_))

    val repos:List[Repo] = json.as[List[models.Repo]]

    logger.trace(s"json parsed ${repos.size}")

    db.run(repodb._deleteAllInOrg(org) andThen
      repodb.bulkInsert(repos)
    )

    logger.trace("All rows inserted")
  }

  def getStats(org: String) = repodb.findByOrg(org)

  def getStatsByForks(org: String, n: Int) = repodb.statsByForks(org, n)
}
