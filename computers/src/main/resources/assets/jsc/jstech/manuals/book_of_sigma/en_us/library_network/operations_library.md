---
section: library_network
order: 449
---
# Operations from a program

An Operation ([](jsc:what_operations_are)) is one job the network does. A SELECT brings an item from the network to this machine, an INSERT sends items the other way, and a CRAFT asks the network to make an item. Operations is the type a program uses to ask for these jobs, and to look at the jobs in flight.

Every Operations call needs a network, so check Network.Online first when you are not sure. A request the network cannot take does not stop the program: Ok is false, and the message says why. The kinds of Operation are in [](jsc:operations_catalogue).

Table: Operations members

| Member | What it does |
| --- | --- |
| `Operations.Pull(item, amount)` | Asks the network to bring the item to this machine's storage (SELECT). |
| `Operations.Push(item, amount)` | Sends items from this machine's storage to the network (INSERT). |
| `Operations.Craft(item, amount)` | Asks the network to make the item (CRAFT). |
| `Operations.Cancel(id)` | Cancels the Operation with that id. |
| `Operations.Reprioritise(id, priority)` | Changes how urgently the Operation runs. |
| `Operations.Update(item, action, amount)` | Asks for an UPDATE with the action, such as SMELT. Added in version 2. |
| `Operations.Get(id)` | The OperationInfo of the Operation with that id, or nothing when it is no longer in flight. |
| `Operations.List()` | Every Operation in flight, as a list of OperationInfo. |
| `Operations.OnSettled(handler)` | Calls the handler with the OperationInfo of each Operation of the network that settles. Added in version 3. |
| `Operations.OnSettled(id, handler)` | Calls the handler once, when the Operation with that id settles. Added in version 3. |

Pull, Push, Craft, Cancel, Reprioritise and Update give an AskResult. Get and List give OperationInfo. Cancel accepts the start of an id, provided that start is at least as long as the short form of the id. Get needs the whole id, and ignores case.

## What the results hold

Table: Records of Operations

| Record | Fields | What each field holds |
| --- | --- | --- |
| `AskResult` | `AskResult.Ok, AskResult.Message` | Ok: yes when the network took the request. Message: the network's answer, in English. |
| `OperationInfo` | `OperationInfo.Id, OperationInfo.Type, OperationInfo.Item, OperationInfo.Moved, OperationInfo.Requested, OperationInfo.Status, OperationInfo.Priority` | Id: the id to use. Type and Status: lower-case text. Item: the item. Moved and Requested: how much has moved, and how much was asked for. |
| `AskResult` | `AskResult.Id` | The short id of the Operation the request set going, which Operations.OnSettled can wait on; empty when it set none going. Added in version 3. |

Table: Priorities of an Operation

| Level | Ways to write it |
| --- | --- |
| Low | `low` |
| Medium low | `medium_low` |
| Medium | `medium, or normal` |
| Medium high | `medium_high` |
| High | `high` |

The five Operation levels in the table are not the same scale as a program's priority, which is written as low, medium or high.

## Messages the network answers with

Table: Messages of an AskResult

| Message | What it means |
| --- | --- |
| unknown item: <item> | The network does not know the item. |
| the network has no running Mainframe | No Mainframe runs on the network. |
| this computer holds no <item> | A push of an item this machine does not hold. |
| nothing to push | The push has no items to send. |
| no pattern crafts <item> | No pattern makes that item. |
| the network has no room for <item> | The network's storage has no room for the item. |
| no operation <id> in flight (see 'ops') | No Operation with that id is running. |
| no such priority: <name> | The priority name is not one of the levels above. |

## Being told when an Operation settles

Operations.OnSettled tells your program when an Operation of the network settles, done or not: its Status says done, partial, failed or discarded. With an id it waits on that one Operation and is told once; Pull, Push and Craft answer with the id in AskResult.Id, so a program can ask for a craft and be told when it is done. Listening costs nothing, and the handlers are kept when the world is saved.

## If something goes wrong

> [!PROBLEM] The program stops and says this computer is not on a network.
> Check Network.Online first.

> [!PROBLEM] AskResult says no pattern crafts the item.
> Add a pattern for the item in the Pattern Studio ([](jsc:pattern_studio)).

> [!PROBLEM] AskResult says the network has no running Mainframe.
> Start a Mainframe on the network, then ask again.

> [!PROBLEM] AskResult says the network has no room for the item.
> Add storage to the network, such as a storage server ([](jsc:storage_servers)).

> [!PROBLEM] Operations.Get gives nothing.
> The Operation has finished or was cancelled. Use Operations.List to see what is running.

> [!SEE] jsc:what_operations_are, jsc:operations_catalogue, jsc:states_and_queues
