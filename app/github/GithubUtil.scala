package github

import javax.inject._

import play.api.Logger
import play.api.mvc._
import play.api.libs.ws._
import play.api.cache.Cached
import dal._
import play.api.libs.json._

import scala.concurrent.{Await, ExecutionContext, Future, Promise}
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
class GithubUtil @Inject()(repo: OrganizationRepository, datadb: OrgDataRepository, repodb: RepoRepository, ws: WSClient, configuration: play.api.Configuration)(implicit ec: ExecutionContext) {
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
    val f = repo.findOrg(org)

    val p = Promise[String]()

    f.map(x => x match {
      case Some(o) => p.success(s"Organization $org exists")
      case None => {
        for {
          _ <- repo.addOrg(org)
        } yield {
          updateOrg(org)
          p.success(s"Organization $org added")
        }
      }
    })

    p.future
  }

  def deleteOrg(org: String) = Future {
    for {
      _ <- repodb.delete(org)
      _ <- datadb.delete(org)
      _ <- repo.delete(org)
    } yield(s"Org $org deleted")
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
    val f = get_data(s"https://api.github.com/orgs/$org/repos")
    f onComplete {
      case Success(json) => {
        logger.trace(s"updateRepos: Got json with length ${json.toString().length}")
        datadb.updateReposJson(org, json)
        repo.updateStats(org, json).map(_ => logger.trace(s"showMembers: Status updated for org = $org"))
      }
      case Failure(ex) => logger.error("Got exception", ex)
    }
  }

  /**
    * Update members json
    * @param org
    * @return
    */
  def updateMembers(org: String) = {
    logger.trace(s"updateMembers: org = $org")
    val f = get_data(s"https://api.github.com/orgs/$org/members")
    f onComplete {
      case Success(json) => {
        logger.trace(s"updateMembers: Got json with length ${json.toString().length}")
        datadb.updateMembersJson(org, json)
      }
      case Failure(ex) => logger.error("Got exception", ex)
    }
  }

  // Test program for webservice call
  def test_ws = {
    //val f = get_data4("https://api.github.com/search/code?q=addClass+user:mozilla")
    //val f = get_data4("https://api.github.com/orgs/Microsoft/repos")
    val f = get_data("https://api.github.com/orgs/parse-community/repos")
    f onComplete {
      case Success(json) => {
        println(s"Got result ${json.toString().substring(0, 100)}")
        println(s"Length ${json.toString().length}")
      }
      case Failure(ex) => logger.error("Got exception", ex)
    }
  }

  /**
    * Evaluate the header and retrieve any links
    * @param resp
    * @return
    */
  private def evaluate_header(resp: StandaloneWSResponse): Option[String] = {
    for (x <- resp.headers.get("Link");
         y <- x;
         m <- keyValPattern.findFirstMatchIn(y)) {

      return Some(m.group(1))
    }
    return None
  }

  /**
    * Evaluate the response and take actions.
    * If the response contains a link, make a recursive call
    * to get more data and return the json with additional data.
    * If there is no link in the response, return json obtained
    * @param resp
    * @return
    */
  private def evaluate_response(resp: StandaloneWSResponse): Future[JsValue] = {
    val body = resp.body
    val json = Json.parse(body)
    //logger.trace(s"Body(${body.length}): ${body.substring(0, 100)}")
    val p = Promise[JsValue]()

    evaluate_header(resp) match {
      case Some(link) => {
        //logger.trace(s"Got link header $link")
        val h = get_data(link)
        h.onComplete {
          case Success(child_json) => {
            //logger.trace(s"Got child json ${child_json.toString().length} for $link")
            p.success(json.as[JsArray] ++ child_json.as[JsArray])
          }
          case Failure(ex) => {
            logger.trace(s"evaluate_response: Got exception $ex")
            p.success(json)
          }
        }
      }
      case _ =>  {
        //logger.trace("No link")
        p.success(json)
      }
    }

    p.future
  }

  /**
    * Utility function to get the data. This will retrieve the link
    * and look to see if there are additional links. If so, it makes
    * a recursive call to get additional data and concatenates it
    * @param url
    * @return
    */
  def get_data(url: String): Future[JsValue] = {
    val p = Promise[JsValue]()
    val wr = ws.url(url)
      .addHttpHeaders("Authorization" -> s"token $token")
    val f = wr.get()

    for {
      resp <- f
      if (resp.status == 200)
    } yield {
      //logger.trace(s"get_data: Got result ${resp} for link ${url}")
      val z = evaluate_response(resp)
      z onComplete {
        case Success(json) => p.success(json)
        case Failure(ex) => p.failure(ex)
      }
    }

    for (
      resp <- f
      if (resp.status != 200)
    ) p.failure(new Exception(s"get_data: Failed with response $resp"))

    for (ex <- f.failed) p.failure(ex)

    p.future
  }
}
