---
section: hardware
order: 28
icon: jsc:furnace_card
covers_family: jsc:expansion_cards
---
# Expansion cards

Besides graphics and sound, the expansion slots of a board take cards that give a computer a job it could not do before. Each card goes in a slot of its own bus family ([the bus family](jsc:build_refusals)).

## Workshop cards

The Crafting Table Card, the Furnace Card, the Enchanting Card and the Anvil Card each give a Personal Computer one job in the Workshop program. Only a Personal Computer takes them, and each one draws 15 W.

The Workshop shows "No fuel" when smelting has no fuel, and "No lapis" when enchanting has no lapis.

## Cluster Interface Cards

Four cards let a Cluster Management Computer reach the machines of your racks: the Serial Console Card, the Management NIC, the Fabric Host Adapter and the Fabric DPU. Each newer one reaches more. A Cluster Management Computer takes one of them ([Cluster Management Computers](jsc:cluster_management_computers)).

Table: The Cluster Interface Cards

| Card | Era and bus | Reaches | Lanes |
| --- | --- | --- | --- |
| Serial Console Card | Vintage, PCI | Rack servers in datacenters | 1 node at a time |
| Management NIC | Legacy, PCI-e 1.0 | Datacenters and supercomputer fabrics | 2 nodes at a time |
| Fabric Host Adapter | Standard, PCI-e 3.0 | Everything, every kind of rack | 4 nodes at a time |
| Fabric DPU | Advanced, PCI-e 4.0 | Everything, every kind of rack | 8 nodes at a time |

> [!DEFINE] Reach
> The kinds of machine a card can reach, from the rack servers of a datacenter up to every kind of rack.

> [!DEFINE] Lane
> The number of nodes a bulk install writes at once. In an older slot the lanes fall, but never below one.

Only a Cluster Management Computer uses these cards.

## Optical Network Card

A network card with a fibre port. A Mainframe or a Cluster Management Computer with one shows its fibre port as connected, so it can join the fibre backbone of the network ([Data cables](jsc:data_cables)). A server never takes it: a rack joins the fibre through a Fibre Optic Interface ([](jsc:rack_equipment)). It is a Standard card, on PCI-e 3.0, and it draws 15 W.

## Integra Phi coprocessors

Four Integra Phi coprocessors add computing power to the nodes of a supercomputer. Only a supercomputer node takes one, and only one per node. A rack server of another kind refuses it ([Server racks and servers](jsc:server_racks)).

Table: The Integra Phi coprocessors

| Coprocessor | Cores and clock | Power | Supercomputer slots |
| --- | --- | --- | --- |
| Integra Phi 5100 Co-processor | 60 cores at 1.05 GHz | 225 W | 1 to 2 |
| Integra Phi 7120 Co-processor | 61 cores at 1.24 GHz | 250 W | 1 to 3 |
| Integra Phi 7290 Co-processor | 72 cores at 1.50 GHz | 270 W | 1 to 4 |
| Integra Phi 9000 Co-processor | 96 cores at 1.80 GHz | 300 W | 1 to 6 |

The last column is the range of supercomputer slots that the tooltip gives, as in "Supercomputer slots 1-2".

## Crafting Cards

The Crafting Cards give a Crafting Computer its crafting. There are six, from the Vintage to the Advanced, and autocrafting explains them ([Crafting Cards](jsc:crafting_cards)).

```table
{"caption": "The Crafting Cards", "heads": ["Era", "Bus", "Power"], "rows": [[{"words": "Vintage"}, {"words": "ISA"}, {"amount": 10, "unit": "W"}], [{"words": "Legacy"}, {"words": "PCI"}, {"amount": 25, "unit": "W"}], [{"words": "Transition"}, {"words": "PCI-e 1.0"}, {"amount": 75, "unit": "W"}], [{"words": "Transition"}, {"words": "PCI-e 2.0"}, {"amount": 100, "unit": "W"}], [{"words": "Standard"}, {"words": "PCI-e 3.0"}, {"amount": 110, "unit": "W"}], [{"words": "Advanced"}, {"words": "PCI-e 4.0"}, {"amount": 125, "unit": "W"}]]}
```

## Storage cards

Vaultis makes a RAID controller and a read cache for each era's slot. In a server they join its drives into one volume and answer their reads faster; in another computer they sit in their slot and do nothing ([RAID modes](jsc:raid_arrays)).

## Network Anchor Cards

A Network Anchor Card keeps the chunks round its computer loaded while the computer runs, so the machines there go on working when you are far away. Any computer takes one, and a server in a rack too: the rack then keeps the chunks round it for the farthest card of its running servers.

```table
{"caption": "The Network Anchor Cards", "heads": ["Era", "Slot", "Keeps loaded", "Draws"], "rows": [[{"words": "Vintage"}, {"words": "ISA"}, {"words": "The computer's chunk"}, {"amount": 5, "unit": "W"}], [{"words": "Legacy"}, {"words": "PCI"}, {"words": "The computer's chunk"}, {"amount": 8, "unit": "W"}], [{"words": "Transition"}, {"words": "PCI-e 2.0"}, {"words": "3 by 3 chunks"}, {"amount": 10, "unit": "W"}], [{"words": "Standard"}, {"words": "PCI-e 3.0"}, {"words": "3 by 3 chunks"}, {"amount": 12, "unit": "W"}], [{"words": "Advanced"}, {"words": "PCI-e 4.0"}, {"words": "5 by 5 chunks"}, {"amount": 15, "unit": "W"}]]}
```

The chunks count against the quota of whoever works the computer: the last player to place it or use it. The server sets that quota, 25 chunks unless it says otherwise, and the card's tooltip shows how much of yours you keep already. When the quota is full the card keeps what fits, nearest first, and the network's log says so.

## WAN Interface Cards

A WAN Interface Card works one port of a WAN Gateway Computer, and only a gateway takes one ([WAN Gateway Computers](jsc:wan_gateways)). There is one for each era's long-distance line, in that era's slot. Each takes the line of its own era and the older ones, and a gateway takes four, one for each of its ports.

```table
{"caption": "The WAN Interface Cards", "heads": ["Card", "Era and bus", "Line", "Speed"], "rows": [[{"words": "Modem Card"}, {"words": "Vintage, ISA"}, {"words": "Telephone Line"}, {"amount": 1, "unit": "it/t"}], [{"words": "T1 Card"}, {"words": "Legacy, PCI"}, {"words": "Leased Line"}, {"amount": 8, "unit": "it/t"}], [{"words": "T3 Card"}, {"words": "Transition, PCI-e 2.0"}, {"words": "T3 Line"}, {"amount": 24, "unit": "it/t"}], [{"words": "VLDC Card"}, {"words": "Standard, PCI-e 3.0"}, {"words": "VLDC"}, {"amount": 64, "unit": "it/t"}], [{"words": "Optical Transport Card"}, {"words": "Advanced, PCI-e 4.0"}, {"words": "Dark Fibre"}, {"amount": 256, "unit": "it/t"}]]}
```

## If something goes wrong

> [!PROBLEM] The Workshop shows "No fuel" or "No lapis".
> Give smelting its fuel, or give enchanting its lapis.

> [!PROBLEM] A Workshop card does not go into a computer.
> Only a Personal Computer takes Workshop cards. Put it in one.

> [!PROBLEM] A second Cluster Interface Card does not go in.
> A Cluster Management Computer takes only one. Take the other one out.

> [!PROBLEM] A Cluster Interface Card does nothing.
> Only a Cluster Management Computer uses it. Put it in one.

> [!PROBLEM] The build says there are too many PCI-e cards.
> Take cards out, or use a board with more PCI-e slots.

> [!PROBLEM] The machines round a computer with a Network Anchor Card stop when you go away.
> The card works only while the computer runs and someone works it. If the network's log says the quota is full, it keeps only what fits: let go of chunks you keep elsewhere.

> [!PROBLEM] A WAN Interface Card does not go into a computer.
> Only a WAN Gateway Computer takes one, four at most.

> [!SEE] jsc:graphics_cards, jsc:sound_cards, jsc:build_refusals
