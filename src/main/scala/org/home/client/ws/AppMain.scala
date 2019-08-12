package org.home.client.ws

import akka.{actor => untyped}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._

object AppMain {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    //val typedSystem: ActorSystem[Any] = ActorSystem(TokenAuthMain(config), "TypedSystem")
    val actorSystem = untyped.ActorSystem("Main")
    actorSystem.actorOf(AppMainActor.props(config), "main_actor")

    sys.addShutdownHook {
      println("shutdown..2")
      actorSystem.terminate()
      Await.ready(actorSystem.whenTerminated, 5 minutes)
    }
  }
}
