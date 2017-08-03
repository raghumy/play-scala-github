package github

import javax.inject._

import play.api.Logger
import play.api.mvc._
import dal._
import play.api.libs.json._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

/**
  * Main controller for Github access.
  * get the latest token.
  * @param repo
  * @param cc
  * @param configuration
  * @param util
  * @param ec
  */
@Singleton
class GithubController @Inject()(repo: OrganizationRepository, cc: ControllerComponents, configuration: play.api.Configuration, util: GithubUtil)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  private val logger = Logger(getClass)

  /**
    * Basic index
    * @return
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


  /**
    * Show all members for this org
    * @param org
    * @return
    */
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

  /**
    * Show all repos for this org
    * @param org
    * @return
    */
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

  /**
    * Show all stats for all repos for this org
    * @param org
    * @return
    */
  def showRepoStats(org: String) = Action.async { implicit request =>
    repo.getStats(org).map { stats =>
      Ok(Json.toJson(stats))
    }
  }

  /**
    * Show stats by type. If a type is not found, an error is generated.
    * @param org
    * @param n
    * @param t
    * @return
    */
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
    * Add an organization to the system. This comes in
    * with a PUT or POST request. If the org exists
    * nothing is done.
    * @param org
    * @return
    */
  def addOrganization(org: String) = Action.async { implicit request =>
    val f = util.addOrg(org)
    for {
      x <- f
      msg <- x
    } yield {
      logger.trace(msg)
      Ok(msg)
    }
  }

  def deleteOrganization(org: String) = Action.async { implicit request =>
    logger.trace(s"Delete $org organization")
    util.deleteOrg(org).map { _ =>
      logger.trace(s"$org organization deleted")
      Ok(s"Organization $org deleted")
    }
  }
}
