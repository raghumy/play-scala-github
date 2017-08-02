package dal

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import play.api.Logger
import java.sql.Timestamp

import models._

import scala.concurrent.{ Future, ExecutionContext }

/**
  * Repository of stats data for the org
  * @param dbConfigProvider
  * @param ec
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
    * Table definition
    */
  private class RepoTable(tag: Tag) extends Table[Repo](tag, "repos") {

    /** The ID column, which is the primary key, and auto incremented */
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name")

    def org = column[String]("org")

    def forks = column[Int]("forks")

    def last_updated = column[Timestamp]("last_updated")

    def open_issues = column[Int]("open_issues")

    def stars = column[Int]("stars")

    def watchers = column[Int]("watchers")

    /**
      * This is the tables default "projection".
      */
    def * = (id, name, org, forks, last_updated, open_issues, stars, watchers) <> ((Repo.apply _).tupled, Repo.unapply)
  }

  /**
    * The starting point for all queries on the organization table.
    */
  private val repos = TableQuery[RepoTable]

  def findByOrg(org: String): Future[List[Repo]] =
    db.run(repos.filter(_.org === org).to[List].result)

  def delete(org: String) = db.run {
    repos.filter(_.org === org).delete
  }

  def statsByForks(org: String, n: Int): Future[List[Repo]] =
    db.run(repos.filter(_.org === org).sortBy(_.forks.desc).take(n).to[List].result)

  def statsByLastUpdated(org: String, n: Int): Future[List[Repo]] =
    db.run(repos.filter(_.org === org).sortBy(_.last_updated.desc).take(n).to[List].result)

  def statsByOpenIssues(org: String, n: Int): Future[List[Repo]] =
    db.run(repos.filter(_.org === org).sortBy(_.open_issues.desc).take(n).to[List].result)

  def statsByStars(org: String, n: Int): Future[List[Repo]] =
    db.run(repos.filter(_.org === org).sortBy(_.stars.desc).take(n).to[List].result)

  def statsByWatchers(org: String, n: Int): Future[List[Repo]] =
    db.run(repos.filter(_.org === org).sortBy(_.watchers.desc).take(n).to[List].result)

  def insert(repo: Repo) =
    repos returning repos.map(_.id) += repo

  def bulkInsert(l:List[Repo]) =
    repos ++= l

  def _deleteAllInOrg(org: String) =
    repos.filter(_.org === org).delete

}
