---
section: library_network
order: 447
---
# Mainframe: what the orchestrator has done

The Mainframe keeps count of the work its network has done: how many of each kind it did, how long each one waited and ran, and how much each one moved.

If you want to know whether a Mainframe runs on the network, ask Mainframe.Online. Its other members need a network, and they are Sigma# only.

Table: Mainframe members

| Member | What it does |
| --- | --- |
| `Mainframe.Online` | Yes when a Mainframe runs on the network. |
| `Mainframe.PeakToday` | The most Operations running at once in the last day. Zero when there is no Mainframe. |
| `Mainframe.Stats(kind)` | The work of one kind, as a WorkStat. |
| `Mainframe.Work()` | The work of every kind the network did, as a list of WorkStat. |

A kind of work the network has not done reads as zeros. Stats reads one kind; Work lists the kinds it has done.

## What a WorkStat holds

Table: Fields of WorkStat

| Field | What it holds |
| --- | --- |
| `WorkStat.Type` | The kind of work, in lower case. |
| `WorkStat.Count` | How many of that kind the network did. |
| `WorkStat.AverageWait` | The average wait of each one, as a whole number. |
| `WorkStat.AverageRun` | The average run of each one, as a whole number. |
| `WorkStat.ShortfallPercent` | A whole number of percent. |
| `WorkStat.Moved` | How much of the item moved, as a long whole number. |

## If something goes wrong

> [!PROBLEM] The program stops and says this computer is not on a network.
> Check Network.Online first.

> [!PROBLEM] Mainframe.PeakToday is 0.
> There is no Mainframe on the network. Check Mainframe.Online.

> [!PROBLEM] Stats gives zeros for a kind of work.
> The network has not done that kind yet. Use Mainframe.Work to see the kinds it has done.

> [!SEE] jsc:mainframes, jsc:operations_catalogue, jsc:states_and_queues
