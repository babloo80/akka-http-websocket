package org.home.client.ws

import java.util.concurrent.TimeUnit

import akka.actor.Scheduler
import akka.actor.typed.ActorRef
import akka.event.Logging
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives.{complete, get, handleWebSocketMessages, logRequestResult, onSuccess, path, _}
import akka.http.scaladsl.server.directives.LogEntry
import akka.http.scaladsl.server.{Route, RouteResult}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import org.home.ws.WSJSONMessageServiceManager
import org.json4s.JsonAST.{JInt, JString, JValue}
import org.json4s.jackson.Serialization
import org.json4s.{DefaultFormats, Formats, jackson}

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, FiniteDuration, _}
import scala.util.Try

object SampleRoutes {
  private val defaultTokenTimeout = 8 hours

  def routes(wsManager: ActorRef[WSJSONMessageServiceManager.Command])(implicit mat: ActorMaterializer, timeout: Timeout, scheduler: Scheduler): Route = {
    import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
    implicit val serialization: Serialization.type = jackson.Serialization // or native.Serialization
    implicit val formats: Formats                  = DefaultFormats
    import akka.actor.typed.scaladsl.AskPattern._

    logRequestResult(logger _) {
      (get & path("ping")) {
        complete("pong")
      } ~ (get & path("ws")) {
        val f: Future[Flow[Message, Message, Any]] = wsManager.ask(ref => WSJSONMessageServiceManager.NewWSFlow(ref))
        onSuccess(f)(handleWebSocketMessages)
      }
    }
  }

  def parseDuration(value: Option[JValue]): Duration = {
    value
      .collect {
        case JInt(v)    => FiniteDuration(v.toLong, TimeUnit.SECONDS)
        case JString(v) => Try(Duration(v)).getOrElse(defaultTokenTimeout)
        case _          => defaultTokenTimeout
      }
      .getOrElse(defaultTokenTimeout)
  }

  private def logger(req: HttpRequest): RouteResult => Option[LogEntry] = {
    case RouteResult.Complete(res) => Some(LogEntry(s"${req.method.name()}: ${res.status.value}", Logging.InfoLevel))
    case RouteResult.Rejected(rej) => Option.empty //todo: write to error.
    case _                         => Option.empty
  }
}
