---
section: computers
order: 52
icon: jsc:mainframe
covers_family: jsc:mainframes
---
# Mainframes

Every network has exactly one Mainframe, and it is the network's control room. Only the Mainframe runs the Network Manager. Two Mainframes on one network are a conflict, so give each network its own.

> [!DEFINE] Mainframe
> The one computer of a network that runs the Network Manager and orchestrates the network's work.

A Mainframe is big: three blocks wide, two tall and two deep. Place a Mainframe block in the middle of the ground floor, on the front row. If the whole 3 by 2 by 2 space is clear, the cabinet forms. If not, the item stays in your hand, and particles outline the blocked spaces so you can see what is in the way.

The Mainframe takes an MTX motherboard of its own era. There is one Mainframe for each era: the Mainframe (Standard), and the Vintage, Legacy, Transition and Advanced Mainframes.

A Mainframe runs the network only with a system on one of its disks, such as MC-NET ([](jsc:mc_net)). Install it as on any computer ([](jsc:installing)); until then the Mainframe holds every Operation it is given. From the Standard on, the backbone is fibre, and the Mainframe takes fibre only while an Optical Network Card sits in one of its card slots ([](jsc:expansion_cards)).

```table
{"caption": "What a Mainframe holds", "rows": [[{"words": "Motherboard"}, {"literal": "1 MTX"}], [{"words": "Processors"}, {"literal": "up to 4"}], [{"words": "Memory"}, {"literal": "up to 8"}], [{"words": "Graphics cards"}, {"literal": "up to 6"}], [{"words": "Disks"}, {"literal": "up to 4"}], [{"words": "Power supply"}, {"literal": "1"}], [{"words": "Storage slots"}, {"literal": "up to 27"}]]}
```

Its hardware sets how fast the network works. Its processors add up to its capacity, and the Subframes that work for it add to that ([](jsc:subframes)). Its memory sets a RAM buffer, counted in items: a Vintage memory module gives 1 to 16 items, and an Advanced module gives 2,048 to 32,768. The Mainframe works at the lower of its capacity and its RAM buffer, so a small buffer holds back fast processors.

> [!DEFINE] Subframe
> A computer that lends its capacity and its queues to the Mainframe of its network.

> [!DEFINE] Capacity
> The items per tick that the Mainframe's processors can handle, added up.

The processors run one queue, and each graphics card runs one more. The queues do not share capacity. A weak card can hold its queue down, and the assembly screen then says "A card's queue runs at most at the CPU's speed, and no faster than the card".

The assembly screen shows these parts of the Mainframe:

Table: The assembly screen

| Part | What it shows |
| --- | --- |
| CAPACITY (it/t) | The items per tick its processors can handle, added up. |
| QUEUES | The queues of its processors and graphics cards. |
| RAM BUFFER (it) | The items its memory can buffer, added up over the modules. |
| AUTO START | Turns the Mainframe on by itself when its build stands. |
| FAILOVER | Switches the Mainframe between owner and standby. |
| PWR, DISK and NET | Three lamps: power, disk and network. |
| STANDBY | This Mainframe waits as a standby. |
| CONFLICT | Two owners share one segment of the network. |

Its power button sits on the sill, the ledge at the bottom of the front, in the middle. A click on it switches the Mainframe on or off, whatever you hold. A click anywhere else on the cabinet opens its assembly screen. Sneak-click the cabinet to open or close the service panel. Its processors, memory and cards are seen only through that opening.

```scene
{"balloons": [{"detail": "Their capacity adds up to the speed of the network.", "number": 1, "part": {"box": [4, 6, 5, 16, 8, 11], "x": 15, "y": 1, "z": 16}, "text": "Processors"}, {"detail": "Its size sets the RAM buffer.", "number": 2, "part": {"box": [4, 1, 5, 15, 5, 11], "x": 15, "y": 1, "z": 16}, "text": "Memory"}, {"detail": "Each card runs one more queue.", "number": 3, "part": {"box": [4, 11, 5, 12, 16, 11], "x": 15, "y": 0, "z": 16}, "text": "Graphics cards"}, {"number": 4, "part": {"box": [4, 6, 5, 11, 11, 11], "x": 15, "y": 0, "z": 16}, "text": "Power supply"}, {"number": 5, "part": {"box": [0, 7, 4, 12, 14, 11], "x": 17, "y": 0, "z": 16}, "text": "Disks"}, {"detail": "Click it to switch the Mainframe on or off.", "number": 6, "part": {"box": [7, 3, 14, 9, 6, 16], "x": 16, "y": 0, "z": 16}, "text": "Power button"}, {"detail": "They show that it runs, that it is on a network, and when it has a fault.", "number": 7, "part": {"box": [0, 9, 14, 11, 11, 16], "x": 17, "y": 1, "z": 16}, "text": "Status lamps"}], "blocks": [{"data": "{VisualHardware:3695079,VisualSystems:0,VisualFlags:15}", "state": "jsc:mainframe[facing=south,optical=false]", "x": 16, "y": 0, "z": 16}, {"state": "jsc:mainframe_part[facing=south,core=false,era=3,optical=false]", "x": 15, "y": 0, "z": 16}, {"state": "jsc:mainframe_part[facing=south,core=false,era=3,optical=false]", "x": 17, "y": 0, "z": 16}, {"state": "jsc:mainframe_part[facing=south,core=false,era=3,optical=false]", "x": 15, "y": 0, "z": 15}, {"state": "jsc:mainframe_part[facing=south,core=true,era=3,optical=false]", "x": 16, "y": 0, "z": 15}, {"state": "jsc:mainframe_part[facing=south,core=false,era=3,optical=false]", "x": 17, "y": 0, "z": 15}, {"state": "jsc:mainframe_part[facing=south,core=false,era=3,optical=false]", "x": 15, "y": 1, "z": 16}, {"state": "jsc:mainframe_part[facing=south,core=true,era=3,optical=false]", "x": 16, "y": 1, "z": 16}, {"state": "jsc:mainframe_part[facing=south,core=false,era=3,optical=false]", "x": 17, "y": 1, "z": 16}, {"state": "jsc:mainframe_part[facing=south,core=false,era=3,optical=false]", "x": 15, "y": 1, "z": 15}, {"state": "jsc:mainframe_part[facing=south,core=true,era=3,optical=false]", "x": 16, "y": 1, "z": 15}, {"state": "jsc:mainframe_part[facing=south,core=false,era=3,optical=false]", "x": 17, "y": 1, "z": 15}], "caption": "A Standard Mainframe, running, with its service panel off so that its parts show. Drag it to turn it.", "height": 130}
```

AUTO START keeps the Mainframe ready. With it on and a valid build, the Mainframe starts at once, as a cold start that runs its self-test. If a change breaks the build, the Mainframe turns off. When the build stands again, it turns on by itself.

With the [FAILOVER](jsc:failover) switch off, the Mainframe is an owner: it takes the network segment it stands on, or lays down a network of its own. With FAILOVER on, it is a standby: when an owner is on its network, it attaches to that network and does not run it.

> [!DEFINE] Owner
> The Mainframe that runs its network, with FAILOVER off.

> [!DEFINE] Standby
> A Mainframe that waits on a network an owner runs, ready to take it over.

A standby takes over a network that has no owner. After 3 seconds, which is 60 ticks, one standby runs the network itself. The countdown is saved with the world, so reloading does not restart it. If two owners sit on one segment of a network, the network goes into conflict, and the screen shows CONFLICT.

If you break a Mainframe, it drops the Mainframe of its own era, and the items in its inventory spill out. If it was the last Mainframe of its network, the network is orphaned, not deleted, so its storage survives ([](jsc:network_identity)).

## If something goes wrong

> [!PROBLEM] The Mainframe does not form.
> Clear the whole 3 by 2 by 2 space, then place it again. The blocked spaces show particles.

> [!PROBLEM] Every Operation stays queued.
> The Mainframe has no system. Install one on it ([](jsc:installing)).

> [!PROBLEM] The fibre does not join the Mainframe.
> Put an Optical Network Card in one of its card slots ([](jsc:expansion_cards)).

> [!PROBLEM] The screen shows CONFLICT.
> Two owners share one segment. Take one of them off the segment, or switch FAILOVER on for one.

> [!PROBLEM] The network is slow.
> Faster processors raise the capacity, and more graphics cards give more queues. Check the memory too: a small buffer holds the capacity back.

> [!PROBLEM] The chat reports a network conflict: two Mainframes share one network.
> Keep one Mainframe on each network, and separate the cables between them.

> [!SEE] jsc:states_and_queues, jsc:failover
