package github

import javax.inject._

import play.api.Logger
import play.api.mvc._
import play.api.libs.ws._
import play.api.cache.Cached
import dal._
import models._
import play.api.libs.json._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class GithubController @Inject()(repo: OrganizationRepository, cached: Cached, cc: ControllerComponents, ws: WSClient, configuration: play.api.Configuration, util: GithubUtil)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  private val logger = Logger(getClass)
  private val token = configuration.underlying.getString("github.token")
  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok("Welcome to Github API")
  }

  /**
    * A REST endpoint that gets all the orgs as JSON.
    */
  def showOrgIndex() = Action.async { implicit request =>
    repo.list().map { orgs =>
      Ok(Json.toJson(orgs))
    }
  }


  def showMembers(org: String) = Action.async { implicit request =>
    logger.trace(s"showMembers: org = $org")
    repo.getOrgData(org).map { od =>
      od match
      {
        case Some(od) => Ok(Json.toJson(od.members_json))
        case None => {
          Ok("Data not found")
        }
      }
    }
  }

  def showRepos(org: String) = Action.async { implicit request =>
    logger.trace(s"showRepos: org = $org")
    repo.getOrgData(org).map { od =>
      od match
      {
        case Some(od) => Ok(Json.toJson(od.repos_json))
        case None => {
          Ok("Data not found")
        }
      }
    }
  }

  def showReposCached(org: String) = cached(implicit request => s"$org/repos", 30) {
    showRepos(org)
  }

  def showRepoStats(org: String) = Action.async { implicit request =>
    repo.getStats(org).map { stats =>
      Ok(Json.toJson(stats))
    }
  }

  def showStatsByForks(org: String, n: String) = Action.async { implicit request =>
    repo.getStatsByForks(org, n.toInt).map { stats =>
      Ok(Json.toJson(stats))
    }
  }

  def showStatsByType(org: String, n: String, t: String) = Action.async { implicit request => {
      t match {
        case "forks" => repo.getStatsByForks(org, n.toInt).map(stats => Ok(Json.toJson(stats)))
        case "last_updated" => repo.getStatsByLastUpdated(org, n.toInt).map(stats => Ok(Json.toJson(stats)))
        case "open_issues" => repo.getStatsByOpenIssues(org, n.toInt).map(stats => Ok(Json.toJson(stats)))
        case "stars" => repo.getStatsByStars(org, n.toInt).map(stats => Ok(Json.toJson(stats)))
        case "watchers" => repo.getStatsByWatchers(org, n.toInt).map(stats => Ok(Json.toJson(stats)))
        case _ => Future(NotFound(s"Stat $t not found"))
      }
    }
  }

  /**
    * The add person action.
    *
    * This is asynchronous, since we're invoking the asynchronous methods on PersonRepository.
    */
  def addOrganization(org: String) = Action.async { implicit request =>
    repo.findOrg(org).map { o =>
      o match {
        case Some(o) => Ok(s"Organization $org exists")
        case None => {
          logger.trace(s"Creating organization $org")
          Await.result(util.addOrg(org), 1 minute)
          /*
          Await.result(repo.create(org).map { _ =>
            util.updateOrg(org)
            Ok(s"Organization $org added")
          }, 1 minute
          */
          Ok(s"Organization $org added")
        }
      }
    }

    /*
    repo.findOrg(org).map(o => match {
      case Some(o) => Ok(s"Organization $org exists")
      case None => {
        logger.trace(s"Creating organization $org")
        repo.create(org).map { _ =>
          Ok(s"Organization $org added")
        }
      }
    }
    )
    */
  }
}
