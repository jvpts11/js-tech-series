---
section: network_operation
order: 103
icon: jsc:mainframe
---
# Failover

A Mainframe can be a failover member. A failover member does not run the network while its owner is there. It waits, powered and ready, and takes over when the owner is gone.

```scene
{"balloons": [{"detail": "Its screen reads ONLINE.", "number": 1, "part": {"x": 1, "y": 0, "z": 1}, "text": "The Mainframe that runs the network"}, {"detail": "Wire both Mainframes to the same segment.", "number": 2, "part": {"x": 4, "y": 0, "z": 1}, "text": "The cable of the segment"}, {"detail": "Turn on its FAILOVER switch. Its screen reads STANDBY.", "number": 3, "part": {"x": 8, "y": 0, "z": 1}, "text": "The standby Mainframe"}], "blocks": [{"data": "{VisualFlags:7}", "state": "jsc:mainframe[facing=south,optical=true]", "x": 1, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:fibre_cable\"}],Links:[I;0,4144,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 3, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:fibre_cable\"}],Links:[I;0,48,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 4, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:fibre_cable\"}],Links:[I;0,48,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 5, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:fibre_cable\"}],Links:[I;0,8240,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 6, "y": 0, "z": 1}, {"data": "{VisualFlags:7}", "state": "jsc:mainframe[facing=south,optical=true]", "x": 8, "y": 0, "z": 1}], "caption": "Two Mainframes on one network: one runs it and the other stands by to take over. Drag it to see them from behind.", "height": 110}
```

Turning failover on removes the network this Mainframe owned, instead of leaving it orphaned. A machine that is about to back somebody else's network cannot keep one of its own, or the two would be in conflict the moment they were wired together.

You make a failover member in three steps, and the screen tells you which role the Mainframe has.

1. Open the Mainframe's screen and turn on the FAILOVER switch.
2. Wire the failover Mainframe to the same segment as its owner.
3. Check that the screen reads STANDBY.

Table: The roles of the Mainframes

| Role | What it does |
| --- | --- |
| Owner, with failover off | Runs the network on its segment. |
| `STANDBY` | Powered and synced, but not running the network. |
| Active, after a takeover | Took over the network, and runs it now. |

A standby takes over only when no owner is running on its segment. It then waits 60 ticks, which is 3 seconds of game time, before it decides that its owner is really gone. The wait gives the owner time to come back first.

When several standbys wait, only the one placed lowest in the world, by its block position, takes over. The others stay on standby.

A standby does not hold a conflict. Only a running owner can put a network into conflict. The wait is kept when the world reloads, so a takeover in progress does not start its wait again.

## If something goes wrong

> [!PROBLEM] The standby never takes over.
> Another standby with a lower position is waiting first, or the standby is not on the owner's segment. A standby has nothing to back if it is not on that segment.

## If something goes wrong

> [!PROBLEM] The screen reads CONFLICT.
> Two running owners share a segment. A standby does not cause a conflict. Separate the owners ([](jsc:network_identity)).
