---
section: clusters
order: 61
icon: jsc:cluster_management_computer
covers_family: jsc:cluster_management_computers
---
# Cluster Management Computers

When you have one rack, you walk up to each server. When you have ten, you will not want to. The Cluster Management Computer installs systems and programs on the machines of your racks, and switches their bays on and off, all from one desk.

> [!DEFINE] Cluster
> A group of machines that one computer manages as one set.

```scene
{"balloons": [{"detail": "It installs systems and programs on the machines of the racks, and switches their bays.", "number": 1, "part": {"x": 18, "y": 0, "z": 15}, "text": "Cluster Management Computer"}, {"number": 2, "part": {"x": 16, "y": 1, "z": 16}, "text": "A Server Rack it manages"}, {"number": 3, "part": {"x": 13, "y": 1, "z": 16}, "text": "A Server Rack it manages"}, {"detail": "It joins the computer to the back of every rack.", "number": 4, "part": {"x": 14, "y": 2, "z": 14}, "text": "Backbone cable"}], "blocks": [{"data": "{Units:[B;1b,10b,0b,4b,0b,1b,12b,11b],DisplayEra:3,BayPowerOff:0,ServicePanelOff:0b}", "state": "jsc:server_rack[facing=south]", "x": 16, "y": 0, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=false]", "x": 16, "y": 0, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=false]", "x": 15, "y": 0, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=false]", "x": 15, "y": 0, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=false]", "x": 16, "y": 1, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=false]", "x": 16, "y": 1, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=false]", "x": 15, "y": 1, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=false]", "x": 15, "y": 1, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=false]", "x": 16, "y": 2, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=false]", "x": 16, "y": 2, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=false]", "x": 15, "y": 2, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=false]", "x": 15, "y": 2, "z": 15}, {"data": "{Units:[B;1b,1b,1b,1b,1b,1b,12b,11b],DisplayEra:3,BayPowerOff:0,ServicePanelOff:0b}", "state": "jsc:server_rack[facing=south]", "x": 13, "y": 0, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=false]", "x": 13, "y": 0, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=false]", "x": 12, "y": 0, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=false]", "x": 12, "y": 0, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=false]", "x": 13, "y": 1, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=false]", "x": 13, "y": 1, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=false,front=true,compute=false]", "x": 12, "y": 1, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=false,front=false,compute=false]", "x": 12, "y": 1, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=false]", "x": 13, "y": 2, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=false]", "x": 13, "y": 2, "z": 15}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=false]", "x": 12, "y": 2, "z": 16}, {"state": "jsc:server_rack_part[facing=south,top=true,front=false,compute=false]", "x": 12, "y": 2, "z": 15}, {"state": "jsc:cluster_management_computer[facing=south,optical=false]", "x": 18, "y": 0, "z": 15}, {"data": "{Wires:[{cable:\"jsc:cx4_cable\"}],Links:[I;0,2088,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 12, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:cx4_cable\"}],Links:[I;0,2104,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 13, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:cx4_cable\"}],Links:[I;0,48,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 14, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:cx4_cable\"}],Links:[I;0,2104,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 15, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:cx4_cable\"}],Links:[I;0,2104,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 16, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:cx4_cable\"}],Links:[I;0,48,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 17, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:cx4_cable\"}],Links:[I;0,17,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 18, "y": 2, "z": 14}, {"data": "{Wires:[{cable:\"jsc:cx4_cable\"}],Links:[I;0,3,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 18, "y": 1, "z": 14}, {"data": "{Wires:[{cable:\"jsc:cx4_cable\"}],Links:[I;0,2058,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 18, "y": 0, "z": 14}], "caption": "A Cluster Management Computer, cabled to the two Server Racks it manages. Drag it to follow the cable behind the racks.", "height": 130}
```

Build it as you would a Personal Computer ([](jsc:personal_computers)), and give it a Cluster Interface Card. Without one, it is an ordinary computer ([](jsc:expansion_cards)). With one, it is the manager of its cluster: each card reaches a different set of machines, and installs on a set number of them at once.

> [!DEFINE] Datacenter
> The Server Racks that one Server Router runs, grouped into sections behind its faces.

Table: Cluster Interface Cards

| Card | Era | Reaches | Nodes at once |
| --- | --- | --- | --- |
| Serial Console Card | Vintage | Server Racks | 1 |
| Management NIC | Legacy | Server Racks and Supercomputer Racks | 2 |
| Fabric Host Adapter | Standard | Every rack type | 4 |
| Fabric DPU | Advanced | Every rack type | 8 |

The card decides what the manager reaches, not the era of the computer. The Serial Console Card of the Vintage era already reaches Server Racks, and every card reaches them.

A job installs on several nodes at once, as the card allows. Each node takes its own install time: 280 ticks at 2 GHz, which is 14 seconds. A faster processor takes less time, but never under 60 ticks, which is 3 seconds, or over 1,200 ticks, which is 60 seconds.

ALL ON and ALL OFF switch every bay of the cluster at once. Each node also has its own bay, which reads "Bay off" when it is switched off.

A system or a program comes from a disc in a reader that is linked to the manager by cable ([](jsc:drives)). The manager looks for a system disc and a program disc in those linked readers.

Table: Messages of the Cluster Manager

| Message | What it means | Fix |
| --- | --- | --- |
| bay off | The node's bay is switched off. | Switch its bay on, or use ALL ON. |
| already installed | The node already has that system or program. | Nothing to do. |
| no system | A program needs a system on the node first. | Install a system on the node first ([](jsc:installing)). |
| too old for the program | The node's era is older than the program allows. | Use a node of a newer era. |
| does not meet the program's requirements | The node lacks what the program asks for. | Use a node that meets them. |
| that system installs by hand from its own shell | The manager cannot install this system. | Install it on that node by hand ([](jsc:installing)). |
| that disc carries nothing to install | The disc holds no system or program. | Put in a disc that holds one. |
| no system disc in a linked reader | No linked reader holds a system disc. | Put a system disc in a reader linked by cable. |
| no program disc in a linked reader | No linked reader holds a program disc. | Put a program disc in a reader linked by cable. |
| a job is already running | Only one job runs at a time. | Wait for it to finish, or cancel it. |
| this card does not reach that cluster | The card's reach stops short of that cluster. | Use a card from the table above that reaches it. |
| no cluster interface card | The computer has no card. | Put a Cluster Interface Card in the computer. |
| changed while writing | The node changed during the job. | Run the job again. |

## If something goes wrong

> [!PROBLEM] The Cluster Manager reaches no machine.
> The computer has no Cluster Interface Card, or the card does not reach the machines. The table above says what each card reaches.

> [!PROBLEM] The manager says "A Cluster Interface Card is required."
> Put a Cluster Interface Card in the computer.

> [!SEE] jsc:expansion_cards, jsc:datacenters
