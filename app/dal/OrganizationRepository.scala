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
class OrganizationRepository @Inject() (dbConfigProvider: DatabaseConfigProvider, repodb: RepoRepository)(implicit ec: ExecutionContext) {
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

    /**
      * This is the tables default "projection".
      *
      * It defines how the columns are converted to and from the Person object.
      *
      * In this case, we are simply passing the name parameters to the Organization case classes
      * apply and unapply methods.
      */
    def * = (id, name) <> ((Organization.apply _).tupled, Organization.unapply)
  }

  /**
    * The starting point for all queries on the organization table.
    */
  private val organization = TableQuery[OrganizationTable]

  /**
    * Create a person with the given name.
    *
    * This is an asynchronous operation, it will return a future of the created person, which can be used to obtain the
    * id for that person.
    */
  def create(name: String): Future[Organization] = db.run {
    // We create a projection of just the name and age columns, since we're not inserting a value for the id column
    (organization.map(p => (p.name))
      // Now define it to return the id, because we want to know what id was generated for the person
      returning organization.map(_.id)
      // And we define a transformation for the returned value, which combines our original parameters with the
      // returned id
      into ((name, id) => Organization(id, name))
      // And finally, insert the person into the database
      ) += (name)
  }

  /**
    * List all the organizations in the database.
    */
  def list(): Future[Seq[Organization]] = db.run {
    organization.result
  }

  private def _findByName(org: String): DBIO[Option[Organization]] =
    organization.filter(_.name === org).result.headOption

  def findOrg(org: String): Future[Option[Organization]] = db.run(_findByName(org))

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

    repodb._deleteAllInOrg(org)

    repos.map(repodb.insert)
  }
}
