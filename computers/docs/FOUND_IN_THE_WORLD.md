# Found in the world

*Added 2026-11-10.*

Before you build a computer of your own, the world already holds some: abandoned buildings with their machines and
their loot, and two villagers who trade in computers. This page covers both, and the settings a server has for them.
The design behind it is in [Survival](design/survival.md).

## Abandoned buildings

Twenty kinds of building lie abandoned across the Overworld, on land. Each is of one era, picked by its distance from
the world's spawn: the Vintage near it, the Advanced far away, among the eras that building may be of.

| Building | Eras | Spacing (chunks) | What you find |
| --- | --- | ---: | --- |
| Abandoned house | Vintage, Legacy | 32 | an old computer in the attic |
| Electronic waste dump | all | 32 | heaps of broken computers in the open, for salvage |
| Overturned delivery van | all | 32 | hardware boxes by the road |
| Computer shop | all | 48 | parts on the shelves, boxed software, a counter |
| LAN house | Legacy, Transition | 48 | many computers on a network, games |
| School computer room | Vintage to Transition | 48 | many computers, educational software |
| Public library | all | 48 | computers, manuals and books |
| Radio and television station | Vintage, Legacy | 48 | old computers and the mast |
| Telephone exchange | all | 48 | routers, cables and rack units |
| University lab | all | 72 | workstations, a small Mainframe, books |
| Bank | all | 72 | a Mainframe in the vault, with its terminals |
| Midsoft office | all | 72 | computers running Frames and Midsoft's boxed software |
| Software house office | all | 72 | the house its sign names, with its own programs |
| Startup building | Transition to Advanced | 72 | a small server room, other houses' software |
| Game studio | Legacy to Advanced | 72 | games and the era's top graphics card |
| Hacker's den | Legacy to Advanced | 72 | a home-made rack in the basement of a house |
| Electronics factory | all | 72 | six of the chain's machines, wafers and chips |
| Crypto mining farm | Standard, Advanced | 72 | mining rigs of graphics cards |
| Datacenter | Legacy to Advanced | 112 | racks of servers, Server Routers, cables |
| Supercomputing centre | Standard, Advanced | 160 | supercomputer racks and Phi coprocessors |

About one in five still works: its machines are whole, their systems installed, and they come up as soon as they
have energy; its battery bank is charged. The rest are ruins, with broken windows, holes in their walls, moss and
vines, their computers open with half their parts in their seats.

- **Loot:** every chest draws 4 to 8 things from its building's table of its era: the era's parts, blank media,
  peripheral cables, what the building is known for, and boxed software (a Midsoft office only Midsoft's).
- **A graphical system is always within reach:** each building's main chest holds a system with its own desktop, or
  in the Vintage UNIX System V and CDE for it.
- **The hardware box** is a carton printed with its era's machine: a few of a computer's parts, or one graphics card.
  It opens once and stays open.

## The Programmer and the IT Technician

A villager who takes a **Programmer's Desk** becomes a Programmer; one who takes a **Repair Bench** becomes an IT
Technician. Both job sites are made at the Crafting Table, two things of the trade over four planks of any wood: a
floppy disk and paper for the desk, solder wire and an iron ingot for the bench.

A villager's level is the era of what it sells: Novice the Vintage, Apprentice the Legacy, Journeyman the Transition,
Expert the Standard, Master the Advanced. It keeps the trades of the levels below.

| Profession | Sells | Buys |
| --- | --- | --- |
| Programmer | systems and programs, on the media of their era | 24 paper, 4 books or 4 blank media for an emerald |
| IT Technician | parts (boards with their firmware written), 8 network cables, monitors | 10 copper or 8 tin ingots for an emerald; a part of an older era for half its era's base |

A price is the era's base times what is sold's factor, at most 64 emeralds: the base is 4, 8, 12, 20 and 32 from the
Vintage to the Advanced, and the factor 1 for a program, a part or a peripheral, 1.5 for a processor or a graphics
card, 2 for a system and a quarter for eight cables. Every trade restocks as vanilla's do after 12 uses, a system after
3.

Every kind of village (plains, desert, savanna, snowy and taiga) may have a **computer shop**, in its own materials,
with both job sites and a Vintage demo computer running MC-DOS, as often as it has a library. With a recipe viewer,
each installer shows who sells it and where it is found.

## Settings

In `jscomputers-server.toml`:

```toml
[survival]
working_ruin_chance = 0.2                # the share of abandoned buildings that still work
era_distances = [750, 1500, 3000, 5000]  # blocks from spawn where the Legacy, Transition, Standard and Advanced begin

[villagers]
programmer = true
it_technician = true
computer_shop = true                     # the computer shop in villages

[structures.datacenter]                  # one table per building
enabled = true
spacing = 112                            # chunks between two of them, on average
eras = ["legacy", "transition", "standard", "advanced"]
```

A change to a building's settings shows in the chunks made afterwards.
