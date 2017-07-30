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
import scala.util.matching.Regex
import scala.collection.mutable._
import scala.collection.mutable.ArrayBuffer

/**
  * This class handles adding an org and updating data from Github. It uses application configuration
  * github.token for making REST calls to the github API
  *
  * @param repo
  * @param datadb
  * @param ws
  * @param configuration
  * @param ec
  */
@Singleton
class GithubUtil @Inject()(repo: OrganizationRepository, datadb: OrgDataRepository, ws: WSClient, configuration: play.api.Configuration)(implicit ec: ExecutionContext) {
  private val logger = Logger(getClass)
  private val token = configuration.underlying.getString("github.token")
  private val duration = configuration.getMillis("github.update.interval")
  private val keyValPattern: Regex = ".*<(.*)>; rel=\"next\".*".r

  /**
    * Update all orgs
    */
  def updateOrgs(): Unit = {
    // Get a list of orgs
    // For each repo, update the map
    val t = new java.sql.Timestamp(new java.util.Date().getTime() - duration)
    logger.trace(s"Getting orgs with last_update < $t (${t.getTime})")
    //repo.list().map(orgs => orgs.map(o => updateOrg(o.name)))
    repo.needUpdate(t).map(orgs => orgs.map(o => updateOrg(o.name)))
  }

  /**
    * Add an org to the system. This will trigger an update of the org
    * to get fresh data. Further updates are picked up by the system.
    *
    * @param org
    * @return
    */
  def addOrg(org: String) = Future {
    logger.trace(s"addOrg: org = $org")
    repo.addOrg(org)
    updateOrg(org)
  }

  def updateOrgHandler(org: String): Unit = {
    val f = repo.updateStateIfEmpty(org, Some("Updating"))
    f.onComplete({
      case Success(v) => updateOrg(org)
      case Failure(t) => logger.trace(s"Org $org cannot be updated because of $t")
    })
  }

  /**
    * Update a single org
    * @param org
    */
  def updateOrg(org: String): Unit = {
    logger.trace(s"updateOrg: org = $org")
    updateRepos(org)
    updateMembers(org)
    repo.updateState(org, None)
  }

  /**
    * Update the repos json. This also reads the json and updates stats based on the results
    * @param org
    * @return
    */
  def updateRepos(org: String) = {
    logger.trace(s"updateRepos: org = $org")
    /*
    val complexRequest: WSRequest = ws.url(s"https://api.github.com/orgs/$org/repos").addHttpHeaders("Authorization" -> s"token $token")

    complexRequest.get().map { response => {
      datadb.updateReposJson(org, response.json)
      repo.updateStats(org, response.json).onComplete {
        case Success(result) => logger.trace(s"showMembers: Status updated for org = $org")
        case Failure(e) => logger.error("Exception", e)
      }
    }
    }
    */

    val f = get_data(s"https://api.github.com/orgs/$org/repos")
    f.map(json => {
      datadb.updateReposJson(org, json)
      repo.updateStats(org, json).map(_ => logger.trace(s"showMembers: Status updated for org = $org"))
    })
  }

  /**
    * Update members json
    * @param org
    * @return
    */
  def updateMembers(org: String) = {
    logger.trace(s"updateMembers: org = $org")
    /*
    val complexRequest: WSRequest = ws.url(s"https://api.github.com/orgs/$org/members").addHttpHeaders("Authorization" -> s"token $token")

    complexRequest.get().map { response => {
      datadb.updateMembersJson(org, response.json)
      }
    }
    */
    val f = get_data(s"https://api.github.com/orgs/$org/members")
    f.map(json => datadb.updateMembersJson(org, json))
  }

  def test_ws = {
    //val f = get_data("https://api.github.com/search/code?q=addClass+user:mozilla")
    val f = get_data("https://api.github.com/orgs/parse-community/repos")
    f.map(json => println(s"Got json list"))
  }

  /**
    * Utility function to get the data. It retrieves up to 5 additional
    * pages if necessary. Can be expanded at a later date for larger data sets
    * TODO: Improve how JSON is combined. Current method requires a lot of memory
    * @param initial_url
    * @return
    */
  def get_data(initial_url: String):Future[JsValue] = Future {
    var url = initial_url
    var jsonList = new ListBuffer[JsValue]()
    // Limit to 5 for now
    for (i <- 1 to 5 if url != null) {
      val wr: WSRequest = ws.url(url)
        .addHttpHeaders("Authorization" -> s"token $token")
      val f = wr.get()
      val g = f.map(resp => {
        logger.trace(s"Got result ${resp} for link ${url}")
        val json = resp.json
        jsonList += json
        //println(s"Current length ${jsonList.length}")
        val l = evaluate_header(resp)
        l match {
          case Some(link) => {
            logger.trace(s"Next link $link")
            url = link
          }
          case _ => {
            //println("No link found")
            url = null
          }
        }
      })
      Await.result(g, 30 seconds)
    }
    //println("Returning value")
    val jsonArray = jsonList.reduceLeft((x, y) => x.as[JsArray] ++ y.as[JsArray])
    //println("Finished combining jsonArray")

    // Return the resulting array
    jsonArray
  }

  def evaluate_header(resp: WSResponse): Option[String] = {
    for (x <- resp.headers.get("Link")) {
      logger.trace(s"Found link header $x")
      for (y <- x) {
        for (patternMatch <- keyValPattern.findAllMatchIn(y)) {
          val link = patternMatch.group(1)
          logger.trace(s"Found match key: $link")
          return Some(link)
        }
      }
    }
    return None
  }
}
