package com.wd.perf.token.auth.http

import akka.actor.typed.ActorRef
import org.home.ws.{IncomingWSMessageRouter, WSJSONMessageService}
import org.json4s.JsonAST
import org.json4s.JsonAST.JObject

class DefaultIncomingWSMessageRouter extends IncomingWSMessageRouter {
  override def routeMessage: PartialFunction[(JsonAST.JValue, ActorRef[WSJSONMessageService.Command]), Unit] = {
    case (message: JObject, sender) =>
      import org.json4s.JsonDSL._
      sender ! WSJSONMessageService.OutgoingJSONMessage(("ack" → true) ~ message)
  }
}
