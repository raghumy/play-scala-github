package github

import javax.inject._
import play.api.Logger
import play.api.mvc._
import play.api.libs.ws._
import play.api.cache.Cached

import scala.concurrent.{ExecutionContext, Future}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class GithubController @Inject()(cached: Cached, cc: ControllerComponents, ws: WSClient, configuration: play.api.Configuration)(implicit ec: ExecutionContext) extends AbstractController(cc) {
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
  def showOrgIndex(org: String) = Action {
    logger.trace(s"showOrgIndex: org = $org")
    Ok(s"showOrgIndex org = $org")
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

    complexRequest.get().map { response => Ok(response.json)}
  }

  def showReposCached(org: String) = cached(implicit request => s"$org/repos", 30) {
    Action.async { implicit request =>
      logger.trace(s"showReposCached: org = $org, token = $token")
      val complexRequest: WSRequest = ws.url(s"https://api.github.com/orgs/$org/repos").addHttpHeaders("Authorization" -> s"token $token")

      complexRequest.get().map { response => Ok(response.json)}
    }
  }
}
