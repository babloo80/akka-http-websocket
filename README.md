# akka-http-websocket
A scala project that provides a generic websocket wrapper to provide easy hookup to an existing actor-system.
The following features are used in making this to work
* akka-http (provides the core for enabling websocket using http route)
* Akka Typed (provides an abstraction for passing messages in a type-safe manner)
* akka-streams (used for generation of `Flow[Message, Message, Any]`)
* json4s (json library that uses `jackson` to provide WS messages as to & fro JSON)

## Using the app


Sample WS req/resp
1. Happy path
##### Request
```json
{"a": "b"}
```
##### Response
```json
{"ack":true,"a":"b"}
```
2. Unsupported request (yet valid JSON)
##### Request
```JSON
"hello"
```
##### Response
```json
{"message-type":"UnsupportedRequestMessage"}
```

3. Invalid JSON
##### Request
```json
bla
```
##### Response
```json
{"message-type":"BadRequestMessage","cause":"Unrecognized token 'bla': was expecting ('true', 'false' or 'null')\n at [Source: (String)\"bla\"; line: 1, column: 7]"}
```
