package org.home.ws

import java.util.UUID

import akka.NotUsed
import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.ws._
import akka.stream.scaladsl.Flow
import akka.stream.{ActorMaterializer, Materializer, OverflowStrategy}
import org.json4s.JsonAST
import org.json4s.JsonAST.{JObject, JValue}
import org.json4s.jackson.JsonMethods._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

/**
  * Handle incoming messages from WS.
  */
trait IncomingWSMessageRouter {

  /**
    * route incoming message
    * @return
    */
  def routeMessage: PartialFunction[(JsonAST.JValue, ActorRef[WSJSONMessageService.Command]), Unit]
}

object WSJSONMessageServiceManager {
  sealed trait Command
  case class NewWSFlow(replyTo: ActorRef[Flow[Message, Message, Any]]) extends Command
  case class Broadcast(message: JValue)                                extends Command
  case class RemoveWSService(serviceId: String)                        extends Command

  def apply(router: IncomingWSMessageRouter)(implicit mat: ActorMaterializer): Behavior[Command] = manage(router, Map.empty)

  private def manage(router: IncomingWSMessageRouter, entries: Map[String, ActorRef[WSJSONMessageService.Command]])(implicit mat: ActorMaterializer): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.receiveMessage[Command] {
      case NewWSFlow(replyTo) =>
        val entry     = context.spawnAnonymous(WSJSONMessageService(router))
        val serviceId = UUID.randomUUID().toString
        context.watchWith(entry, RemoveWSService(serviceId))
        replyTo ! wsJSONMessageHandler(entry)
        context.log.info(s"Adding WS Service: $serviceId")
        manage(router, entries + (serviceId -> entry))

      case RemoveWSService(serviceId) =>
        context.log.info(s"Removing WS Service: $serviceId")
        manage(router, entries.filterNot { case (key, _) => key == serviceId })

      case Broadcast(message) =>
        entries.foreach { case (_, each) => each ! WSJSONMessageService.OutgoingJSONMessage(message) }
        Behaviors.same
    }
  }

  def wsJSONMessageHandler(actorRef: ActorRef[WSJSONMessageService.Command])(implicit mat: ActorMaterializer): Flow[Message, Message, Any] = {
    import akka.stream.typed.scaladsl._
    val incoming = Flow[Message]
      .mapAsync(1)(_.removeStream(5 minutes))
      .collect { case v: Message => WSJSONMessageService.IncomingMessage(v) }
      .to(ActorSink.actorRef[WSJSONMessageService.Command](actorRef, WSJSONMessageService.Stop, WSJSONMessageService.ReportError))

    //This outgoing ActorSource is error-ing out to the declared RuntimeException.
    val outgoing = ActorSource
      .actorRef[Message]({ case TextMessage.Strict("close") => }, {
        case null =>
          new RuntimeException(s"Null cannot be sent}")
      }, 10, OverflowStrategy.dropHead)
      .mapMaterializedValue(v => actorRef ! WSJSONMessageService.OutgoingActorRef(v))

    Flow.fromSinkAndSource(incoming, outgoing)
  }

  implicit class RichMessage(value: Message) {
    def removeStream(duration: FiniteDuration)(implicit mat: Materializer): Future[Any] = {
      value match {
        case v: TextMessage   => v.toStrict(duration)
        case v: BinaryMessage => v.toStrict(duration)
        case _                => Future.successful(NotUsed)
      }
    }
  }
}

object WSJSONMessageService {
  sealed trait Command
  case class IncomingMessage(v: Message)                  extends Command
  case class IncomingJSONMessage(v: JValue)               extends Command
  case class OutgoingJSONMessage(v: JValue)               extends Command
  case class OutgoingActorRef(ref: ActorRef[TextMessage]) extends Command
  case class ReportError(err: Throwable)                  extends Command
  case object Stop                                        extends Command

  def apply(router: IncomingWSMessageRouter): Behavior[Command] = handleMessages(router, null)

  private def handleMessages(router: IncomingWSMessageRouter, outgoingHandler: ActorRef[TextMessage]): Behavior[Command] = Behaviors.setup { ctx =>
    ctx.setReceiveTimeout(5 minutes, Stop)

    Behaviors.receiveMessage {
      case IncomingJSONMessage(v) =>
        ctx.log.info(s"Incoming message: ${compact(v)}")
        val route = router.routeMessage
        if (route.isDefinedAt(v, ctx.self)) {
          Try(route.apply(v, ctx.self)).recover {
            case th: Throwable => ctx.log.error(th, s"Error occurred while processing incoming message: ${compact(v)}")
          }
        } else {
          ctx.log.warning(s"Unsupported Incoming message: ${compact(v)}")
          import org.json4s.JsonDSL._
          v match {
            case jobj: JObject => ctx.self ! OutgoingJSONMessage(jobj ~ ("message-type" -> "UnsupportedRequestMessage"))
            case _             => ctx.self ! OutgoingJSONMessage(("message-type"        -> "UnsupportedRequestMessage"))
          }
        }
        Behaviors.same

      case IncomingMessage(TextMessage.Strict(text)) =>
        Try(parse(text)) match {
          case Success(input) => ctx.self ! WSJSONMessageService.IncomingJSONMessage(input)
          case Failure(exception) =>
            import org.json4s.JsonDSL._
            ctx.log.error(exception, s"Error while processing: '$text'")
            ctx.self ! OutgoingJSONMessage((("message-type" -> "BadRequestMessage") ~ ("cause" -> exception.getMessage)))
        }
        Behaviors.same

      case IncomingMessage(unknown) =>
        ctx.log.warning(s"Unsupported incoming message: ${unknown}")
        Behaviors.same

      case OutgoingJSONMessage(v) =>
        ctx.log.info(s"Outgoing message: ${compact(v)}")
        if (outgoingHandler == null) {
          ctx.log.warning(s"Outgoing message handler not set. Skipping: ${compact(v)}")
        }
        outgoingHandler ! TextMessage(compact(v))
        Behaviors.same

      case OutgoingActorRef(ref) =>
        ctx.log.info(s"Registration of outgoing handler")
        handleMessages(router, ref)

      case Stop =>
        ctx.log.info("Stopping actor!")
        Behaviors.stopped

      case ReportError(th) =>
        ctx.log.error(th, "Error occurred while receiving message from WS")
        Behaviors.stopped
    }
  }
}
