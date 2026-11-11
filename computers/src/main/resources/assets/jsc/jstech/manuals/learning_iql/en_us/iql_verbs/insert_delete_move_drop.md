---
section: iql_verbs
order: 368
---
# INSERT, DELETE, MOVE and DROP

These four verbs move items or destroy them. INSERT brings items into the network. DELETE sends them out of the network. MOVE carries them from one place to another. DROP destroys them. Where you type a quantity, it is a number, ALL, or nothing.

A bus is a block that moves items between the network and a chest or machine beside it. That chest or machine is the bus's external inventory ([](jsc:buses)).

Table: The four item verbs

| Verb | Form and what it does |
| --- | --- |
| `INSERT` | INSERT [quantity] item [FROM bus]. Brings the items this computer holds into the network, or imports them from a bus. |
| `DELETE` | DELETE [quantity] item TO bus. Sends the items the network holds out to a bus. |
| `MOVE` | MOVE [quantity] item FROM source TO destination. Carries items between two servers, or to and from a bus. |
| `DROP` | DROP [quantity] item. Destroys the items the network holds. |

## INSERT

Without FROM, INSERT pushes what this computer holds into the network. With FROM and the name of a bus, it imports from that bus instead. Whatever the network cannot keep goes back to the bus.

Table: The replies of INSERT

| Reply | What it means |
| --- | --- |
| `INSERT queued: <quantity> <item> -> network` | The items are on their way into the network. |
| `nothing to push` | There is nothing for INSERT to push. |
| `this computer holds no <item>` | This computer has none of the item. Put it into this computer first. |
| `MOVE queued: import from <bus>` | The import has started. The reply says MOVE, although you typed INSERT: that is the network's own wording. |
| `nothing to import from <bus>` | The bus holds nothing to import. |
| `the bus '<bus>' touches no inventory` | The bus has no chest or machine beside it to take from. |

## DELETE

DELETE sends items out of the network. It needs TO and the name of a bus, and a server name is not valid there. Only items the network holds are sent, so a second DELETE does nothing once the stock runs out. DELETE accepts FROM but ignores it.

Table: The replies of DELETE

| Reply | What it means |
| --- | --- |
| `DELETE queued: <quantity> <item>` | The items are on their way out to the bus. |
| `nothing to delete` | The network holds none of the item. |
| `no bus named '<bus>'` | No bus has that name on the network. Check the name in the bus's window. |

## MOVE

MOVE needs both FROM and TO. Between two servers, it moves items inside the network. A bus after TO sends items out, and a bus after FROM brings them in.

Table: The replies of MOVE

| Reply | What it means |
| --- | --- |
| `MOVE queued: <quantity> <item> -> <bus>` | The items are on their way to that bus. |
| `nothing to move` | The source holds none of the item. |
| `nothing to move to <bus>` | The source holds nothing to send to that bus. |
| `no server or bus named '<name>'` | Neither a server nor a bus has that name. |
| `the destination server is unavailable` | The destination cannot take items now. |
| `could not start the MOVE` | The network did not start the move. |

## DROP

> [!WARNING] A DROP destroys the items for good. They leave the network and are not kept.

DROP takes no TO. It destroys only the items the network holds, so you cannot drop what you do not have on the network. It runs as you type it, with no question, as on a real database. The Management Studio asks only before a DELETE with ALL.

Table: The replies of DROP

| Reply | What it means |
| --- | --- |
| `DROP queued: <quantity> <item>` | The items are being destroyed. |
| `nothing to drop` | The network holds none of the item. |

## If something goes wrong

> [!PROBLEM] DELETE exports out of the network and needs a TO destination
> Add TO and the name of a bus.

> [!PROBLEM] MOVE is internal and needs both FROM and TO
> Name the place to take from after FROM, and the place to put the items in after TO.

> [!PROBLEM] DROP has no TO destination
> Remove the TO. DROP only destroys items.

> [!PROBLEM] expected a location after FROM, got: <text>
> Type the name as one word, with no space.

> [!SEE] jsc:buses, jsc:set_bus, jsc:query_and_select
