---
section: gateway
order: 168
---
# The jsc_gateway peripheral

A ComputerCraft program reaches the Gateway through its peripheral, which has the type jsc_gateway. The program wraps it with peripheral.find("jsc_gateway") and calls its methods. Each call counts against the call cap ([](jsc:gateway_permissions)).

Names in the calls are the names the game uses. An item is an id such as iron_ingot, and the minecraft: namespace may be left out for vanilla items. A fluid is written fluid/ and its id, and a chemical chemical/ and its id.

## Reading methods

Table: Reading methods, which need the read switch

| Method | What it does |
| --- | --- |
| `capacity()` | Returns a number: the capacity of the network. |
| `used()` | Returns a number: how much of the capacity is in use. |
| `types()` | Returns a table of each name and its total. |
| `total(name)` | Returns a number: how much of that name the network holds. |
| `find(name)` | Returns a list of entries, each with a server and the quantity it holds. |
| `servers()` | Returns a list of servers, each with its name, used and capacity, and whether it is online. |
| `computers()` | Returns a list of the computers on the network, each with its name, label, operating system, type and online state. |
| `operation(id)` | Returns the table of one operation, or nil when there is none. |
| `operations()` | Returns a list of the operations, the running ones and those settled in the past hour. |
| `watch(name)` | Returns the total now, and watches the name, so that a jsc_stock event comes when it moves. |
| `unwatch(name)` | Returns true or false: true when the watch ends. |

## Operation methods

Table: Operation methods, which need the operations switch

| Method | What it does |
| --- | --- |
| `pull(name, quantity, priority)` | Starts a pull that brings items into the buffer, and returns the id of the operation. |
| `push(name, quantity, priority)` | Starts a push of items from the buffer to the network, and returns the id. |
| `craft(name, quantity, priority)` | Starts a craft of the item, and returns the id. |
| `cancel(id)` | Stops an operation, and returns true or false. |
| `run(computer, program, args...)` | Starts a program on another computer, and returns its process number. |

The priority argument is optional. The quantity 0 means all ([](jsc:gateway_permissions)).

An operation has a long id. You may give only its first letters, as long as no other operation starts with the same letters.

## Status and log methods

Table: Status and log methods

| Method | What it does |
| --- | --- |
| `online()` | Returns true when the Gateway is linked to a host. |
| `name()` | Returns the name of the Gateway. |
| `host()` | Returns the name of the host computer, or an empty text while not linked. |
| `send(text)` | Says a line to the host, and returns true or false. |
| `log(level, text)` | Writes a line to the Gateway's log. The level is info, warn or error. |

A line written to the log is cut at 96 characters, and the log keeps 40 entries.

## If something goes wrong

> [!PROBLEM] A pulled fluid or chemical starts an operation, but nothing arrives in the buffer.
> The buffer takes items only. Pull an item instead.

> [!PROBLEM] A push names a fluid or a chemical, and the text says "the buffer holds no fluid/<id>" or "the buffer holds no chemical/<id>".
> A push takes items from the buffer only. Push an item name.

> [!PROBLEM] The text says "no such method <name> on <peripheral>".
> The method name is not one of those in the tables on this page. Check the spelling.

> [!PROBLEM] The text says "more than one operation starts with" the letters you gave.
> Give more of the id, until only one operation starts with them.

> [!SEE] jsc:operations_catalogue
