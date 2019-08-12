# akka-http-websocket
A scala project that provides a generic websocket wrapper to provide easy hookup to an existing actor-system.
The following features are used in making this to work
* akka-http (provides the core for enabling websocket using http route)
* Akka Typed (provides an abstraction for passing messages in a type-safe manner)
* akka-streams (used for generation of `Flow[Message, Message, Any]`)
* json4s (json library that uses `jackson` to provide WS messages as to & fro JSON)
