The is a chat application (a canonical use case for WebSocket) built using the [Java WebSocket API (JSR 356)](jcp.org/en/jsr/detail?id=356). This is meant to help understand the API usage and try it hands on (this is not a full-blown chat service)

## Components

- Uses the [Tyrus](https://tyrus.java.net) implementation for the core logic
- Leverages the [Grizzly](https://grizzly.java.net/) container support available in Tyrus
- Packaged as a standalone JAR 

## Build & run

The application is a Maven project

- To build, just execute `mvn clean install` which will produce an independent (fat/uber) JAR
- Run it using `java -jar target/websocket-chat.jar`

## Features

Here is what you can do with the chat application

- Join the chat room
- Send public messages
- Send private messages
- Get notified about new users joining
- Leave the chat room
- Get notified another user leaves the chat room

## Code

Before you explore the source code yourself, here is quick overview

|Class(es)|Category|Description|
|---------|--------|-----------|		
|`ChatServer`|Core|It contains the core business logic of the application|
|`WebSocketServerManager`|Bootstrap|Manages bootstrap and shutdown process of the WebSocket container|
|`ChatMessage`,<br>`DuplicateUserNotification`,<br>`LogOutNotification`,<br>`NewJoineeNotification`,<br>`Reply`,<br>`WelcomeMessage`|Domain objects|Simple POJOs to model the application level entities|
|`ChatMessageDecoder`|Decoder|Converts chats sent by users into Java (domain) object which can be used within the application|
|`DuplicateUserMessageEncoder`<br>,`LogOutMessageEncoder`,<br>`NewJoineeMessageEncoder`,<br>`ReplyEncoder`,<br>`WelcomeMessageEncoder`|Encoder(s)|Converts Java (domain) objects into native (text) payloads which can be sent over the wire using the WebSocket protocol|


> **Unit tests**: The unit tests use the Java client API implementation in Tyrus

## Try it out

You would need a WebSocket client for this example - try the [Simple WebSocket Client](https://chrome.google.com/webstore/detail/simple-websocket-client/pfdhoblngboilpfeibdedpjgfnlcodoo?hl=en) which is a Chrome browser plugin. Here is a transcript

- Users **foo** and **bar** join the chatroom. To do so, you need to connect to the WebSocket endpoint URL e.g. `ws://localhost:8080/chat/foo/` (do the same for user **bar**). **foo** gets notified about **bar**
- User **john** joins (`ws://localhost:8080/chat/john/`). **foo** and **bar** are notified
- **foo** sends a message to everyone (public). Both **bar** and **john** get the message
- **bar** sends a private message to **foo**. Only **foo** gets it
- In the meanwhile, **john** gets bored and decides to leave the chat room. Both **foo** and **bar** get notified
