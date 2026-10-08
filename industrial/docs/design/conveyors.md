# Conveyors

How items move without J's Computers: belts, their parts, pneumatic tubes and bulk storage.

J's Industrial moves items **by pushing**: whoever has an item sends it somewhere. It never knows what is stored
elsewhere, never fetches an item on request and never crafts on its own. That is J's Computers' work: its network keeps
an index of everything stored, brings what is asked for and runs autocrafting ([Computers](computers.md)). With both
mods installed, each keeps its job.

## What exists today

Nothing: items move only through vanilla hoppers and other mods' pipes.

## To build

### Belts

Belts move items in sight, one lane per belt. Each block draws energy, its motors' share.

| Belt | Tier | Flow | Draw per block | In real life |
| --- | --- | --- | --- | --- |
| Rubber Belt Conveyor | T1 | 1 item a tick | 100 W | Thomas Robins' rubber belt for mines, 1891 |
| Aluminium Belt Conveyor | T2 | 4 | 300 W | |
| Steel Cord Belt Conveyor | T3 | 16 | 800 W | the steel cord belts of the 1950s |
| Aramid Belt Conveyor | T4 | 64 | 2 kW | belts reinforced with aramid ([Chemistry](chemistry.md)) |

- When the way out is blocked, items **back up**, as on a real belt.
- Players and mobs standing on a belt are carried along.
- A belt keeps its items as **compressed queues**, not as entities, so thousands of items on belts weigh nothing on the
  server's ticks.

### Parts

| Block | Tier | What it does |
| --- | --- | --- |
| Splitter | T1 | one belt into two, in turn |
| Merger | T1 | two belts into one |
| Pusher | T1 | pushes from the belt into the inventory beside it |
| Puller | T1 | pulls from the inventory beside it onto the belt |
| Bucket Elevator | T1 | lifts items straight up, 1 to 16 blocks, as the grain elevators of the 1880s |
| Photoelectric Sensor | T2 | gives a redstone pulse for each item that passes: it counts and detects |
| Filter Splitter | T2 | turns aside one chosen item |
| RFID Sorter | T4 | sorts by tag instead of item by item |

### Pneumatic tubes

Belts carry a lot over a short way; **pneumatic tubes** carry less, far. Capsules are shot through tubes by compressed
air, as Paris' pneumatic post (1866) and the tubes of department stores and hospitals did.

- **Stations have an address.** A sending station takes items (from a Puller, a machine, a belt) and sends them in a
  **capsule** to a receiving station, which unloads into an inventory or onto a belt.
- **Routing by filter**, pushing only: iron goes to station 3, coal to station 7, everything else to station 1.
- **Capsules are physical.** A capsule carries up to 4 stacks, a station sends one a second, and the trip takes time by
  distance, about 20 blocks a second.
- The air comes from the **Compressed Air Line** ([Machines](machines.md)); without air, the stations stop.
- Capsule tubes are their own line of J's Core's cable block.
- With J's Computers installed, the network can tell a station to send a capsule, through the Industrial Controller
  Computer ([Computers](computers.md)). The asking, the index and the crafting stay J's Computers'.

### Bulk storage

For whoever plays without J's Computers:

- the **Silo** (T1, a multiblock of variable size) holds one item by the thousand, as grain and cement silos;
- the **Crate** (T1) is a big box of one item.

Robot arms, guided vehicles and forklifts belong to J's Robotics and J's Transport.
