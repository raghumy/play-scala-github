package github

import javax.inject._

import play.api.Logger
import play.api.mvc._
import play.api.libs.ws._
import play.api.cache.Cached
import dal._
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
    val complexRequest: WSRequest = ws.url(s"https://api.github.com/orgs/$org/members").addHttpHeaders("Authorization" -> s"token $token")

    complexRequest.get().map { response => Ok(response.json)}
  }

  def showMembersCached(org: String) = cached(implicit request => s"$org/members", 30) {
    Action.async { implicit request =>
      logger.trace(s"showMembersCached: org = $org, token = $token")
      val complexRequest: WSRequest = ws.url(s"https://api.github.com/orgs/$org/members").addHttpHeaders("Authorization" -> s"token $token")

      complexRequest.get().map { response => Ok(response.json)}
    }
  }

  def showRepos(org: String) = Action.async { implicit request =>
    logger.trace(s"showMembers: org = $org")
    val complexRequest: WSRequest = ws.url(s"https://api.github.com/orgs/$org/repos").addHttpHeaders("Authorization" -> s"token $token")

    complexRequest.get().map { response => {
      /*
      Await.ready(repo.updateStats(org, response.json).map { _ =>
        logger.trace(s"showMembers: Status updated for org = $org")
      })
      repo.updateStats(org, response.json).onComplete {
          case Success(result) => logger.trace(s"showMembers: Status updated for org = $org")
          case Failure(e) => logger.error("Exception", e)
      }
      */
      Ok(response.json)
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
          Await.result(repo.create(org).map { _ =>
            util.updateRepos(org)
            Ok(s"Organization $org added")
          }, 1 minute
          )
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
