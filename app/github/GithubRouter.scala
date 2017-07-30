package github

import javax.inject.Inject

import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

/**
  * Routes and URLs to the Github controller.
  */
class GithubRouter @Inject()(controller: GithubController) extends SimpleRouter {
  val prefix = "/github"

  override def routes: Routes = {
    case GET(p"/") =>
      controller.index

    case GET(p"/orgs") =>
      controller.showOrgIndex()

    case PUT(p"/orgs/$org") =>
      controller.addOrganization(org)

    case POST(p"/orgs/$org") =>
      controller.addOrganization(org)

    case GET(p"/orgs/$org/members") =>
      controller.showMembers(org)

    case GET(p"/orgs/$org/repos") =>
      controller.showRepos(org)

    case GET(p"/views/$org/stats") =>
      controller.showRepoStats(org)

    case GET(p"/views/$org/top/$n/$t") =>
      controller.showStatsByType(org, n, t)
  }

}
