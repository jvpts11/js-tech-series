---
section: network_operation
order: 102
icon: jsc:mainframe
---
# Network identity, conflicts and orphans

Every network has an identity, which tells it apart from every other network. You never type that identity; the network shows a short form of it.

A Mainframe with no network yet lays down a new network of its own. A Mainframe that is wired into a segment already on a network adopts that network. A segment is the cables joined through a Mainframe, bridged into one.

Table: Words on the Mainframe's screen

| Word | What it means |
| --- | --- |
| `ONLINE` | The Mainframe is running. |
| `OFFLINE` | The build is not valid, so it cannot run. |
| `READY` | The build is valid, and it is not running yet. |
| `STANDBY` | It is a failover member, waiting ([](jsc:failover)). |
| `CONFLICT` | The network is in conflict, shown in red. |
| `LINKED` | The Mainframe is joined to a network. |
| `--` | The Mainframe has no network. |

Two running Mainframes on one segment are a conflict. The network collapses until they are pulled apart: both stop being the network's owner, and nothing is deleted. Only a running Mainframe counts as a rival; one that is switched off does not.

When a conflict starts, the chat reads "[J's Computers] NETWORK_CONFLICT: two Mainframes share one network near", followed by the position of the Mainframe. When the conflict is over, the chat reads "[J's Computers] Network conflict resolved near" and the position.

Once the second Mainframe is gone from the segment, the one that is left runs the network again, and the conflict clears.

When the last Mainframe of a network is broken, the network is orphaned. An orphaned network keeps its storage, so the storage is still there when somebody plugs a new Mainframe in. Switching a Mainframe off does not orphan its network: the data stops passing through it.

> [!DEFINE] Orphaned network
> A network with no Mainframe left. Its storage is still there.

> [!DEFINE] Failover
> A Mainframe that backs up the owner of its network, and takes over when that owner is gone.

> [!DEFINE] Standby
> A failover Mainframe that waits, powered and ready, without running the network.

## If something goes wrong

> [!PROBLEM] The screen says CONFLICT, and both Mainframes stop.
> Two Mainframes share one segment. Remove the cable that joins their segments, so that each network has one Mainframe.

## If something goes wrong

> [!PROBLEM] The network is orphaned after you broke a Mainframe.
> That Mainframe was the last one of its network, so no Mainframe runs it now. Its storage is kept, as described in the section above.
