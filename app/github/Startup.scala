package github

import javax.inject._

import play.api.inject.ApplicationLifecycle

import scala.concurrent.{ExecutionContext, Future}
import akka.actor._
import play.api.Logger

import scala.concurrent.duration._

trait Startup {
  def hello(): Unit
  def goodbye(): Unit
}

/**
  * This class handles startup initialization. The main function of this class is to configure an
  * asynchronous task that updates the repository with data for various orgs.
  * It uses an github.update.interval application parameter for the duration.
  *
  * @param actorSystem
  * @param appLifecycle
  * @param util
  * @param configuration
  * @param ec
  */
@Singleton
class StartupImpl @Inject()(actorSystem: ActorSystem, appLifecycle: ApplicationLifecycle, util: GithubUtil, configuration: play.api.Configuration)(implicit ec: ExecutionContext) extends Startup {
  private val logger = Logger(getClass)

  override def hello(): Unit = {
    val d = Duration(configuration.getMillis("github.check.interval"), MILLISECONDS)
    logger.error(s"Application started with check interval $d ")
    //util.test_ws
    actorSystem.scheduler.schedule(initialDelay = 0.seconds, interval = d) {
      util.updateOrgs()
    }
  }
  override def goodbye(): Unit = logger.error("Goodbye!")

  // You can do this, or just explicitly call `hello()` at the end
  def start(): Unit = hello()

  // When the application starts, register a stop hook with the
  // ApplicationLifecycle object. The code inside the stop hook will
  // be run when the application stops.
  appLifecycle.addStopHook { () =>
    goodbye()
    Future.successful(())
  }

  // Called when this singleton is constructed (could be replaced by `hello()`)
  start()
}
