package github

import javax.inject.Inject

import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

/**
  * Routes and URLs to the PostResource controller.
  */
class GithubRouter @Inject()(controller: GithubController) extends SimpleRouter {
  val prefix = "/github"

  override def routes: Routes = {
    case GET(p"/") =>
      controller.index

    case GET(p"/orgs/$org/") =>
      controller.showOrgIndex(org)

    case GET(p"/orgs/$org/members") =>
      controller.showMembersCached(org)

    case GET(p"/orgs/$org/repos") =>
      controller.showReposCached(org)
  }

}
