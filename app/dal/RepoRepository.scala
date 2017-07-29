package dal

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import play.api.Logger

import models._

import scala.concurrent.{ Future, ExecutionContext }

/**
  * A repository for people.
  *
  * @param dbConfigProvider The Play db config provider. Play will inject this for you.
  */
@Singleton
class RepoRepository @Inject() (dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  // We want the JdbcProfile for this provider
  protected val dbConfig = dbConfigProvider.get[JdbcProfile]
  private val logger = Logger(getClass)

  // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
  // The second one brings the Slick DSL into scope, which lets you define the table and other queries.
  import dbConfig._
  import profile.api._

  /**
    * Here we define the table. It will have a name of organization
    */
  private class RepoTable(tag: Tag) extends Table[Repo](tag, "repos") {

    /** The ID column, which is the primary key, and auto incremented */
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name")

    def org = column[String]("org")

    def forks = column[Int]("forks")

    def last_updated = column[Long]("last_updated")

    def open_issues = column[Int]("open_issues")

    def stars = column[Int]("stars")

    def watchers = column[Int]("watchers")

    /**
      * This is the tables default "projection".
      *
      * It defines how the columns are converted to and from the Person object.
      *
      * In this case, we are simply passing the name parameters to the Repo case classes
      * apply and unapply methods.
      */
    def * = (id, name, org, forks, last_updated, open_issues, stars, watchers) <> ((Repo.apply _).tupled, Repo.unapply)
  }

  /**
    * The starting point for all queries on the organization table.
    */
  private val repos = TableQuery[RepoTable]

  def findByOrg(org: String): Future[List[Repo]] =
    db.run(repos.filter(_.org === org).to[List].result)

  def insert(repo: Repo) = db.run {
    repos returning repos.map(_.id) += repo
  }

  def _deleteAllInOrg(org: String) = db.run {
    repos.filter(_.org === org).delete
  }
}
