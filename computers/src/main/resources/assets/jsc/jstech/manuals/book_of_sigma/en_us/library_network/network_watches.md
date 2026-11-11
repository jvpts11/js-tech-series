---
section: library_network
order: 445
---
# The network: what it holds, and being told when it changes

The network is every computer and device joined by data cable to one Mainframe, the machine that runs the network's operations. A program on a machine that is on the network can ask what the network holds, and it can ask to be told when that changes.

A machine that is on no network cannot ask any of this. Only Network.Online and Network.Current answer there, and every other call stops the program.

Table: Network members

| Member | What it does |
| --- | --- |
| `Network.Online` | Yes when the machine is on a network. |
| `Network.Current` | The short id of the network the machine is on, or null when it is on none. |
| `Network.Capacity` | What the network's servers can hold, as a long whole number. |
| `Network.Used` | What the network's servers hold, as a long whole number. |
| `Network.Total(item)` | The number of that item on the whole network. |
| `Network.Types()` | The names of the items the network holds, as a list. |
| `Network.Find(item)` | A list of HoldingInfo, one for each server that holds the item. |
| `Network.Servers()` | A list of ServerInfo, one for each server. |
| `Network.Watch(item, handler)` | Calls the handler when the network total of the item changes. Gives a Subscription. |
| `Network.WatchBelow(item, threshold, handler)` | Calls the handler when the total falls to the threshold or below it. Gives a Subscription. |
| `Network.WatchAbove(item, threshold, handler)` | Calls the handler when the total rises to the threshold or above it. Gives a Subscription. |
| `Network.Computers()` | A list of the other computers on the network, as RemoteComputer values. |
| `Network.Computer(host)` | The computer with that host name, or null when there is none. |
| `Network.OnEvent(handler)` | Calls the handler with a NetworkEvent each time an entry is written to the network's event log. Added in version 3. |

Every member of Network is available in Sigma# only. Total counts every server that holds the item.

## What the records hold

Table: Records of Network

| Record | Fields | What each field holds |
| --- | --- | --- |
| `HoldingInfo` | `HoldingInfo.Server, HoldingInfo.Quantity` | The name of a server, and how much of the item it holds. |
| `ServerInfo` | `ServerInfo.Name, ServerInfo.Stored, ServerInfo.Capacity` | The name of a server, what it holds, and what it can hold. |
| `StockEvent` | `StockEvent.Item, StockEvent.Total, StockEvent.Previous` | The item, its new total, and its total before the change. |
| `Subscription` | `Subscription.Id, Subscription.Item` | The number of the watch, and the item it watches. |
| `NetworkEvent` | `NetworkEvent.Level, NetworkEvent.Machine, NetworkEvent.Text, NetworkEvent.Tick` | How serious the entry is (info, warn or error), the machine it is about, what it says, and the tick it was written on. |

## Watches

A watch is kept by your program, not by the machine. Once each tick the machine is asked for the totals of everything being watched, and the watches that fire run in the order you set them.

A watch does not fire on its first reading. A program that starts with a low total is not told that it fell. WatchBelow fires once when the total goes to the threshold or below, after it was above. It rearms when the total goes back above, so it can fire again. WatchAbove works the other way round.

The library has no call that stops a watch. Watches are kept when the world is saved and loaded again.

## The network's events

Network.OnEvent tells your program of each entry written to the network's event log, as it is written: a UPS gone on battery, a power supply that degraded, a conflict between Mainframes. One handler hears every entry, so a later call to it takes the place of the one before. Listening costs nothing, the handler runs on your program's credits, and it is kept when the world is saved, as a watch is.

> [!DEFINE] Network total
> The sum of an item over every server on the network.

> [!DEFINE] Threshold
> The number a watch is set at. Below and above fire on crossing it, not while the total stays there.

> [!DEFINE] Rearm
> A watch is ready to fire again once the total has gone back to the other side of the threshold.

## If something goes wrong

> [!PROBLEM] The program stops and says this computer is not on a network.
> Check Network.Online first, and call the other members only when it says yes.

> [!PROBLEM] Network.Current gives null.
> The machine is on no network. Check Network.Online first.

> [!PROBLEM] Network.Computer gives null.
> No computer on the network has that host name. Look at the names in Network.Computers.

> [!PROBLEM] A watch does not fire after you set it.
> A watch fires on a change after its first reading. Wait for the total to change.

> [!SEE] jsc:what_a_network_is, jsc:first_network, jsc:network_identity
