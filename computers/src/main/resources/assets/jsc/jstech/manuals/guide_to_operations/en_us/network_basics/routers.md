---
section: network_basics
order: 99
icon: jsc:standard_router
covers_family: jsc:routers
---
# Routers and repeaters

A router is where the lines of a network meet. Each era has its own, and a router joins its era's access line to its backbone: your small computers on one side, the Mainframe and the racks on the other. It takes the cables of its own era and of every earlier one, on any of its six faces, and all of them become one network. A router extends a network; it never splits one. When your network must reach further, you add a router.

Table: The routers

| Router | Era | What it takes |
| --- | --- | --- |
| Vintage Router | Vintage | The Vintage cables |
| Personal Router | Legacy | The Legacy cables, and the Vintage ones |
| Transition Router | Transition | The Transition cables, and the older ones |
| Standard Router | Standard | The Standard cables, and the older ones |
| Advanced Router | Advanced | The Advanced cables, and the older ones |
| Optical Router | Standard | The backbone's fibre only |
| Advanced Optical Router | Advanced | The backbone's fibre only |

Eras matter here. A port takes its own era's cable and every older one, never a newer one. Two eras of one line that touch do not join: they meet at a router of the newer era. So an old machine on a new network needs a router between them.

From the Standard on, the backbone's fibre runs only straight. An optical router takes only the backbone's fibre, from the Standard's era up to its own, and it is the block that turns and branches fibre.

A repeater renews a cable's range. Lay it in a run of cable, and the cable after it starts counting its range again, so the run can go on past its first range. A repeater takes every line of its era and of every earlier one, except the long distance line. It carries each line on its own and never joins one line to another; that is a router's work.

```scene
{"balloons": [{"detail": "It joins the access line to the backbone.", "number": 1, "part": {"x": 0, "y": 0, "z": 0}, "text": "The Standard Router"}, {"detail": "It carries the backbone far, but only straight.", "number": 2, "part": {"x": 1, "y": 0, "z": 0}, "text": "The Fibre Optic Cable"}, {"detail": "Lay it in a long run to renew the range of the cable.", "number": 3, "part": {"x": 3, "y": 0, "z": 0}, "text": "The Standard Repeater"}, {"detail": "Turn and branch the fibre here.", "number": 4, "part": {"x": 6, "y": 0, "z": 0}, "text": "The Optical Router"}, {"detail": "The access cables of the far computers join it.", "number": 5, "part": {"x": 6, "y": 0, "z": 3}, "text": "The router at the far end"}], "blocks": [{"state": "jsc:standard_router", "x": 0, "y": 0, "z": 0}, {"data": "{Wires:[{cable:\"jsc:fibre_cable\"}],Links:[I;0,4144,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 1, "y": 0, "z": 0}, {"data": "{Wires:[{cable:\"jsc:fibre_cable\"}],Links:[I;0,8240,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 2, "y": 0, "z": 0}, {"state": "jsc:standard_repeater", "x": 3, "y": 0, "z": 0}, {"data": "{Wires:[{cable:\"jsc:fibre_cable\"}],Links:[I;0,4144,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 4, "y": 0, "z": 0}, {"data": "{Wires:[{cable:\"jsc:fibre_cable\"}],Links:[I;0,8240,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 5, "y": 0, "z": 0}, {"state": "jsc:optical_router", "x": 6, "y": 0, "z": 0}, {"data": "{Wires:[{cable:\"jsc:fibre_cable\"}],Links:[I;0,1036,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 6, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:fibre_cable\"}],Links:[I;0,2060,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 6, "y": 0, "z": 2}, {"state": "jsc:standard_router", "x": 6, "y": 0, "z": 3}], "caption": "Routers, a repeater and optical fibre carrying a network far. The fibre runs straight, so the Optical Router turns it. Drag it to see it from the other side.", "height": 120, "view": {"zoom": 1.35}}
```

Hubs are not data blocks. A hub belongs to the peripheral line: it takes one of its computer's device ports and offers ports of its own, so one computer can reach more devices. A hub does not carry the network ([](jsc:peripheral_cables)).

Table: The hubs

| Hub | Ports it offers |
| --- | --- |
| Vintage Hub | 2 ports |
| Legacy Hub | 4 ports |
| Transition Hub | 4 ports |
| Standard Hub | 7 ports |
| Advanced Hub | 7 ports |

The Server Router is a different block, with its own page ([](jsc:server_router)).

## If something goes wrong

> [!PROBLEM] An older machine will not join a newer network.
> Put a router of the newer era between them.

> [!PROBLEM] The far end is still off the network.
> A run on one side of the repeater is still longer than its range. Add a repeater inside it.
