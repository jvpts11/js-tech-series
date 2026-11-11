---
section: computers
order: 53
icon: jsc:wan_gateway
covers_family: jsc:wan_gateways
---
# WAN Gateway Computers

A WAN Gateway Computer stands at either end of a long-distance line: the Telephone Line, the Leased Line, the T3 Line, the VLDC and the Dark Fibre, one for each era ([Data cables](jsc:data_cables)). It is a computer like any other. It takes a board, processors, memory, a power supply and disks, and it runs a system. What makes it a gateway is its WAN Interface Cards.

> [!DEFINE] WAN Gateway Computer
> The computer at either end of a long-distance line, with a port for the line for each of its WAN Interface Cards.

A gateway is a block wider than a Mainframe: four blocks wide, two tall and two deep. Place it on the bottom front row, second from the left as you face it. If the whole 4 by 2 by 2 space is clear, the cabinet forms. If not, the item stays in your hand, and particles outline the blocked spaces.

There is one gateway for each era: the WAN Gateway Computer (Standard), and the Vintage, Legacy, Transition and Advanced ones. Each takes an MTX motherboard of its own era, as a Mainframe does.

```table
{"caption": "What a gateway holds", "rows": [[{"words": "Motherboard"}, {"literal": "1 MTX"}], [{"words": "Processors"}, {"literal": "up to 4"}], [{"words": "Memory"}, {"literal": "up to 8"}], [{"words": "Expansion cards"}, {"literal": "up to 6, of them up to 4 WAN Interface Cards"}], [{"words": "Disks"}, {"literal": "up to 4"}], [{"words": "Power supply"}, {"literal": "1"}]]}
```

Its four ports for the long-distance line are on its back, one in the middle of each lower block, numbered 1 to 4 from the left as you stand behind it. Each port takes the line of the gateway's era and of every earlier era. The first WAN Interface Card in its card slots works port 1, the second works port 2, and so on, so a port with no card does nothing.

A line is up while the gateways at both of its ends run Nouvell BorderLink, both ports are on, and each card takes the line between them. A card takes the line of its own era and the older ones, as a port does.

> [!DEFINE] WAN Interface Card
> The card that works one port of a WAN Gateway Computer, for the long-distance line of its era and the older ones.

A gateway is no orchestrator. It joins the network it stands on through the backbone, on any face, as a Mainframe does, and it takes fibre only with an Optical Network Card. It never joins a network through a long-distance line: that line goes to the far end.

The assembly screen is the Mainframe's, with the lines in place of the operations:

Table: The assembly screen

| Part | What it shows |
| --- | --- |
| CAPACITY (it/t) | The items per tick its processors can handle, added up. |
| LINES | How many of its lines are up, of the ports with a card, as "2 / 3 UP". |
| NETWORK | LINKED while it is on a network. |
| AUTO START | Turns the gateway on by itself when its build stands. |
| LINES lamps 1 to 4 | Each port's line: green when up, red when down, dark with no card. |
| PWR, DISK and NET | Three lamps: power, disk and network. |

```scene
{"balloons": [{"detail": "One for each WAN Interface Card, its line's lamps down its front.", "number": 1, "part": {"box": [0, 6, 11, 13, 16, 13], "x": 18, "y": 0, "z": 16}, "text": "Line modules"}, {"detail": "Click it to switch the gateway on or off.", "number": 2, "part": {"box": [7, 3, 14, 9, 6, 16], "x": 16, "y": 0, "z": 16}, "text": "Power button"}, {"detail": "They show that it runs, that it is on a network, and when it has a fault.", "number": 3, "part": {"box": [0, 9, 14, 11, 11, 16], "x": 17, "y": 1, "z": 16}, "text": "Status lamps"}], "blocks": [{"data": "{VisualSystems:0,VisualFlags:15,Lines:37}", "state": "jsc:wan_gateway[facing=south,optical=false]", "x": 16, "y": 0, "z": 16}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=0]", "x": 18, "y": 0, "z": 16}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=0]", "x": 17, "y": 0, "z": 16}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=0]", "x": 15, "y": 0, "z": 16}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=1]", "x": 18, "y": 0, "z": 15}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=2]", "x": 17, "y": 0, "z": 15}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=3]", "x": 16, "y": 0, "z": 15}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=4]", "x": 15, "y": 0, "z": 15}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=0]", "x": 18, "y": 1, "z": 16}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=0]", "x": 17, "y": 1, "z": 16}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=0]", "x": 16, "y": 1, "z": 16}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=0]", "x": 15, "y": 1, "z": 16}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=0]", "x": 18, "y": 1, "z": 15}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=0]", "x": 17, "y": 1, "z": 15}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=0]", "x": 16, "y": 1, "z": 15}, {"state": "jsc:wan_gateway_part[facing=south,era=3,optical=false,port=0]", "x": 15, "y": 1, "z": 15}], "caption": "A Standard WAN Gateway Computer, running, with three WAN Interface Cards: two lines up and one down. Drag it to turn it.", "height": 130}
```

The cabinet shows its lines too. Its line bay has a line module for each card, with the port's number at the top and its lamps down the front, and a blank where no card is.

Its power button sits on the sill in the middle of the block you placed. A click on it switches the gateway on or off. A click anywhere else on the cabinet opens its assembly screen, and a sneak-click opens or closes the service panel.

If you break a gateway, it drops the gateway of its own era, and the items in its inventory spill out.

Nouvell BorderLink is the gateway's own service, with a version for each era. Without it the gateway's lines stay down. On a Frames desktop it is a window. On MC-NET it fills the screen, with Ports, Log and Console on its rail. At the prompt, the borderlink command does the same things.

BorderLink lists the four ports: the line in each, the gateway at the far end, what the line does, and whether it is up. Pick a port to see what the far side may do here, whether the port relays for other networks, and whether the port is on.

What a line does depends on where the Mainframes are. With a Mainframe on one side only, the line extends that network over the other place: the computers, racks and cables there join it, as if one cable ran between them. With a Mainframe on each side, the two networks federate. They stay two networks, each with its own storage, and each works with the other as far as the other allows.

> [!DEFINE] Extension
> A long-distance line with a Mainframe on one side only: the network reaches over the line into the other place.

> [!DEFINE] Federation
> A long-distance line with a Mainframe on each side: two networks that stay two, and work with each other as far as each allows.

> [!DEFINE] Nouvell BorderLink
> The WAN Gateway Computer's own service, which carries its long-distance lines and says what each far side may do here.

Each port decides what the network at its far end may do here: see our items, take items, send items, and craft here. A new port lets the far side see, and nothing more. Relay lets the networks behind the far side reach through this one, up to three lines away.

A federated network shows in the Network Interactor's Federated tab, in a view of its own. From there you can take its items into your computer, drop a stack on its grid to send it there, and have it craft something, which stays on that network. Each of these crosses the line as an Operation, at the speed of the slowest line on the way, and the Operations tab says where it has got to.

A line that goes down while items cross it loses them, when the server is set to lose items when a network splits. When it is not, the Operation waits for the line to come back. An Operation whose far side is not loaded waits for its chunks.

## If something goes wrong

> [!PROBLEM] The gateway does not form.
> Clear the whole 4 by 2 by 2 space, then place it again. The blocked spaces show particles.

> [!PROBLEM] A WAN Interface Card does not go into a Mainframe or a Personal Computer.
> Only a WAN Gateway Computer takes one.

> [!PROBLEM] A fifth WAN Interface Card does not go in.
> The gateway has four ports, so it takes four cards. Put another kind of card in the rest.

> [!PROBLEM] A line stays down.
> Check that the gateway runs, that the cable is in the port of that card, and that the card is as new as the line. Both gateways must run BorderLink, with the port on.

> [!PROBLEM] The line is up, but neither network sees the other's items.
> The two federate only with a Mainframe on each side. Then check, in the far gateway's BorderLink, what its port lets this network do.

> [!PROBLEM] A Mainframe across an extension does not make a network conflict.
> That is how it should be: it makes a federation. A network conflict comes only from two Mainframes in the same place.

> [!SEE] jsc:mainframes, jsc:expansion_cards, jsc:network_interactor
