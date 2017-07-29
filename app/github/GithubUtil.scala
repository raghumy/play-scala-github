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
class GithubUtil @Inject()(repo: OrganizationRepository, datadb: OrgDataRepository, ws: WSClient, configuration: play.api.Configuration)(implicit ec: ExecutionContext) {
  private val logger = Logger(getClass)
  private val token = configuration.underlying.getString("github.token")

  def updateOrgs(): Unit = {
    // Get a list of orgs
    // For each repo, update the map
    repo.list().map(orgs => orgs.map(o => updateOrg(o.name)))
  }

  def addOrg(org: String) = Future {
    logger.trace(s"addOrg: org = $org")
    repo.addOrg(org)
    updateOrg(org)
  }

  def updateOrg(org: String): Unit = {
    logger.trace(s"updateOrg: org = $org")
    updateRepos(org)
    updateMembers(org)
  }

  def updateRepos(org: String) = {
    logger.trace(s"updateRepos: org = $org")
    val complexRequest: WSRequest = ws.url(s"https://api.github.com/orgs/$org/repos").addHttpHeaders("Authorization" -> s"token $token")

    complexRequest.get().map { response => {
      datadb.updateReposJson(org, response.json)
      repo.updateStats(org, response.json).onComplete {
        case Success(result) => logger.trace(s"showMembers: Status updated for org = $org")
        case Failure(e) => logger.error("Exception", e)
      }
    }
    }
  }

  def updateMembers(org: String) = {
    logger.trace(s"updateMembers: org = $org")
    val complexRequest: WSRequest = ws.url(s"https://api.github.com/orgs/$org/members").addHttpHeaders("Authorization" -> s"token $token")

    complexRequest.get().map { response => {
      datadb.updateMembersJson(org, response.json)
      }
    }
  }
}
