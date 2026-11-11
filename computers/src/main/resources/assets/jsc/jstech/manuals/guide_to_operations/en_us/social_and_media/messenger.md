---
section: social_and_media
order: 89
icon: jsc:personal_computer
---
# Messenger

Messenger lets the people on a network talk to each other. Two programs make it work. On the desktop, the program is called Midsoft Messenger, and it is the window you talk in. The Messenger Service keeps the conversations, and it runs on a server in a rack.

> [!DEFINE] Midsoft Messenger
> The desktop window of Messenger, where you read and type your messages.

> [!DEFINE] Messenger Service
> The service on a rack server that keeps a network's conversations for every Midsoft Messenger on it.

The Messenger Service installs only on a server in a rack of the network ([](jsc:server_racks)). A Personal Computer or a Mainframe cannot host it.

## The window

Table: Parts of Messenger

| Part | What it does |
| --- | --- |
| Roster | The people on the network, under On the network; Everybody is the one room they all read. |
| Conversation | What has been said, on the right. |
| Compose line | Where you type a message. |
| Send | Sends the message. |
| Nudge | Sends a nudge to the others. |

Press [[Enter]] to send a message. A message holds at most 200 characters. The window asks the server for news every 2 seconds. When you nudge, the other computers show a line that says a nudge was sent, with the name of the sender.

## Limits and cost

The service keeps the last 500 messages. When there are more, the oldest fall off the end. Its history lives on the server, not on either computer, so a conversation is there when you sit at any computer of the network. It goes with the server if the server is taken away.

The service costs 8 MB of memory before anyone connects, and 2 MB more for each person who is connected. Each message takes its own text and a little overhead. The bar at the foot of the roster shows what the service weighs.

## Deleting conversations

Whoever is at the server that runs the Messenger Service deletes the conversations it keeps. There, the roster shows a Kept conversations button, whose window lists every conversation with who took part, how many lines it holds, its size and when it was last spoken in, and deletes one or all of them. A deleted conversation leaves every Messenger at once, without a word to anybody.

The server's prompt does the same with msgsvc: msgsvc list numbers the conversations, msgsvc delete 2 deletes the second, msgsvc delete --all deletes every one, and msgsvc status says what the service keeps and weighs ([](jsc:prompt_mainframe_verbs)).

## If something goes wrong

> [!PROBLEM] The window says No Messenger Service on this network.
> Install the Messenger Service on a server in a rack of this network, and let it run.

> [!SEE] jsc:server_racks, jsc:server_router, jsc:mainframes
