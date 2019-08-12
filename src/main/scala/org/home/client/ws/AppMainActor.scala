package org.home.client.ws
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorSystem, Props, Scheduler}
import akka.http.scaladsl._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.ServerSettings
import akka.stream.ActorMaterializer
import akka.util.Timeout
import akka.{actor ⇒ untyped}
import com.typesafe.config.Config
import com.wd.perf.token.auth.http.DefaultIncomingWSMessageRouter
import org.home.ws.WSJSONMessageServiceManager

import scala.concurrent.duration._
import scala.util.{Failure, Success}

case class AppMainActor(config: Config) extends untyped.Actor with untyped.ActorLogging {
  private implicit val system: ActorSystem    = context.system
  private implicit val mat: ActorMaterializer = ActorMaterializer()
  private implicit val timeout: Timeout       = Timeout(5 minutes)
  private implicit val scheduler: Scheduler   = system.scheduler
  import context.dispatcher

  private val wsManager: ActorRef[WSJSONMessageServiceManager.Command] = context.spawn(WSJSONMessageServiceManager(new DefaultIncomingWSMessageRouter()), "ws_manager")

  private val httpFuture = Http().bindAndHandleAsync(Route.asyncHandler(SampleRoutes.routes(wsManager)), config.getString("app.http.name"), config.getInt("app.http.port"), settings = ServerSettings(system))

  sys.addShutdownHook {
    httpFuture.onComplete {
      case Success(bind) =>
        log.info(s"HTTP ${bind.localAddress.getHostName} ${bind.localAddress.getPort} is going down.")
        bind.unbind()
      case Failure(exception) => log.error(exception, "Error occurred while stopping HTTP.")
    }
  }

  override def receive: Receive = {
    case _ =>
  }
}

object AppMainActor {
  def props(config: Config): Props = {
    Props(AppMainActor(config))
  }
}
