---
section: clusters
order: 62
---
# Supercomputers

A supercomputer is every Supercomputer Rack tied together by a fabric cable, meeting the network at one HBW Interface ([](jsc:hbw_interface)). The Standard and the Advanced have an interface of their own.

The fabric cables are the InfiniBand Cable (Transition), the High Compute Cable (Standard) and the OSFP Cable (Advanced).

```scene
{"balloons": [{"detail": "A full computer of two rack units, with its own co-processor.", "number": 1, "part": {"box": [6, 4, 13, 16, 14, 16], "x": 13, "y": 2, "z": 16}, "text": "Supercomputer Node"}, {"number": 2, "part": {"x": 14, "y": 1, "z": 16}, "text": "Supercomputer Rack"}, {"detail": "It ties every rack of the supercomputer to the interface.", "number": 3, "part": {"x": 12, "y": 2, "z": 14}, "text": "High Compute Cable"}, {"detail": "The one point where the supercomputer meets the network.", "number": 4, "part": {"x": 16, "y": 0, "z": 14}, "text": "HBW Interface"}, {"number": 5, "part": {"x": 17, "y": 0, "z": 14}, "text": "Backbone cable"}, {"number": 6, "part": {"x": 18, "y": 0, "z": 14}, "text": "A router of the network"}], "blocks": [{"data": "{Units:[B;6b,0b,6b,0b,6b,0b,6b,0b],DisplayEra:3,BayPowerOff:0,ServicePanelOff:1b}", "state": "jsc:supercomputer_rack[facing=south]", "x": 14, "y": 0, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=true]", "x": 14, "y": 0, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=true]", "x": 13, "y": 0, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=true]", "x": 13, "y": 0, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=true]", "x": 14, "y": 1, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=true]", "x": 14, "y": 1, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=true]", "x": 13, "y": 1, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=true]", "x": 13, "y": 1, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=true]", "x": 14, "y": 2, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=true]", "x": 14, "y": 2, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=true]", "x": 13, "y": 2, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=true]", "x": 13, "y": 2, "z": 15}, {"data": "{Units:[B;6b,0b,6b,0b,6b,0b,6b,0b],DisplayEra:3,BayPowerOff:0,ServicePanelOff:1b}", "state": "jsc:supercomputer_rack[facing=south]", "x": 11, "y": 0, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=true]", "x": 11, "y": 0, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=true]", "x": 10, "y": 0, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=true]", "x": 10, "y": 0, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=true]", "x": 11, "y": 1, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=true]", "x": 11, "y": 1, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=true]", "x": 10, "y": 1, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=true]", "x": 10, "y": 1, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=true]", "x": 11, "y": 2, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=true]", "x": 11, "y": 2, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=true]", "x": 10, "y": 2, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=true]", "x": 10, "y": 2, "z": 15}, {"data": "{Wires:[{cable:\"jsc:hpc_cable\"}],Links:[I;0,0,0,0,2088,0,0,0,0]}", "state": "jscore:cable", "x": 10, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:hpc_cable\"}],Links:[I;0,0,0,0,2104,0,0,0,0]}", "state": "jscore:cable", "x": 11, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:hpc_cable\"}],Links:[I;0,0,0,0,48,0,0,0,0]}", "state": "jscore:cable", "x": 12, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:hpc_cable\"}],Links:[I;0,0,0,0,2104,0,0,0,0]}", "state": "jscore:cable", "x": 13, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:hpc_cable\"}],Links:[I;0,0,0,0,2104,0,0,0,0]}", "state": "jscore:cable", "x": 14, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:hpc_cable\"}],Links:[I;0,0,0,0,17,0,0,0,0]}", "state": "jscore:cable", "x": 15, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:hpc_cable\"}],Links:[I;0,0,0,0,3,0,0,0,0]}", "state": "jscore:cable", "x": 15, "y": 1, "z": 14}, {"data": "{Wires:[{cable:\"jsc:hpc_cable\"}],Links:[I;0,0,0,0,8226,0,0,0,0]}", "state": "jscore:cable", "x": 15, "y": 0, "z": 14}, {"state": "jsc:hbw_interface", "x": 16, "y": 0, "z": 14}, {"data": "{Wires:[{cable:\"jsc:cx4_cable\"}],Links:[I;0,12336,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 17, "y": 0, "z": 14}, {"state": "jsc:standard_router", "x": 18, "y": 0, "z": 14}], "caption": "A supercomputer: two Supercomputer Racks of nodes, joined by High Compute Cable to the HBW Interface, which meets the network. Drag it to see the cables behind the racks.", "height": 130, "view": {"zoom": 1.35}}
```

Each Supercomputer Rack holds Supercomputer Nodes, which are full computers. A node adds crafts only when it holds a Phi that fits its slot, is assembled, and has its bay on. The Integra Phi co-processors come in four models.

> [!DEFINE] Co-processor
> A card that adds extra processing to a node, such as the Integra Phi series.

Table: Integra Phi co-processors

| Co-processor | Slots it fits | Cores | Power draw |
| --- | --- | --- | --- |
| Integra Phi 5100 | 1 to 2 | 60 cores at 1.05 GHz | 225 W |
| Integra Phi 7120 | 1 to 3 | 61 cores at 1.24 GHz | 250 W |
| Integra Phi 7290 | 1 to 4 | 72 cores at 1.5 GHz | 270 W |
| Integra Phi 9000 | 1 to 6 | 96 cores at 1.8 GHz | 300 W |

A Phi's tooltip gives its slots as "Supercomputer slots 1-2" for the Integra Phi 5100. The nodes are counted across the racks on the run, from the top down, and the first six nodes are the rated slots, numbered 1 to 6.

Each slot gives parallel crafts: slot 1 gives 8 crafts, slot 2 gives 16 crafts, doubling up to slot 6 with 256 crafts. Together the six slots give up to 504 parallel crafts. Each slot shows its state:

Table: What a slot shows

| Slot shows | What it means |
| --- | --- |
| NO PHI CARD | The node holds no Phi. |
| RATING LOW | The Phi is rated only for the slots before this one. |
| BAY OFF | The node's bay is switched off. |
| NO NODE | No node sits in that slot. |

The supercomputer is ONLINE when its interface is on a network and at least one slot runs crafts. A slot runs crafts when its node holds a Phi that fits, it is assembled, and its bay is on. An operation can hold several slots, as many as are free, and the Mainframe shows the crafts in use out of the total.

## If something goes wrong

> [!PROBLEM] The supercomputer is not ONLINE.
> Check the slots: each needs a node with a Phi that fits, is assembled, and has its bay on.

> [!PROBLEM] A slot says RATING LOW.
> Use a Phi whose rated slots reach that far.

> [!PROBLEM] A node past six adds nothing.
> Only the first six nodes count, in rack order from the top. Put the ones you want first.

> [!SEE] jsc:server_racks, jsc:hbw_interface
