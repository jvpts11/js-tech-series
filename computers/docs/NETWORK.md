# The network

What a network of computers does for you: keeps your items, moves them, crafts them, and how you ask it to. For
how to build one, see [Getting started](GETTING_STARTED.md#your-first-network); for its cables, [Cables](CABLES.md).

## The Mainframe

*Added 2026-06-05.*

A network is every computer and device joined by data cable to one **Mainframe**. The Mainframe is its brain: it
keeps the index of everything the network stores, takes every request, decides which of its queues works it, and
moves the items. Two Mainframes on one network are a conflict; give each network its own.

How fast the whole network is comes from the Mainframe's hardware ([Hardware](HARDWARE.md#the-computers)): its
processors set how many items it handles at once, its memory how many can wait on their way in or out, and each
graphics card adds a queue of work done alongside the others.

The **Network Manager**, which only runs on the Mainframe, is the network's control room: every machine on it,
the storage, and the Operations being worked.

**Subframes** (added 2026-11-09). A Mainframe's network engine plays one of two roles, set on the Network Manager's
Services tab or at the prompt with `iqlengine role subframe` and `iqlengine role orchestrator`. The **orchestrator**
runs the network. A **Subframe** counts for no network conflict, keeps no index and runs no Operations of its own: it
lends the orchestrator its share of capacity (`balance.subframe_efficiency_factor`) and its graphics cards' queues
when both run the same engine, and is a reserve when it runs another. When the orchestrator falls and nobody runs the
network for 60 ticks, a Mainframe in failover takes it over first; otherwise a Subframe running the same engine, then
one running another, the lowest-placed first. The network's engine becomes the new orchestrator's, the fallen one's
Operations are lost, and the event log says who stopped answering, who took the network over, which engine runs now
and how many Operations were discarded. The Services tab lists every Subframe with what it lends and says who would
take the network over. Giving a Subframe the orchestrator's role on a network that already has one is a conflict.

**The network's event log** (added 2026-10-25), in the Network Manager's Log tab beside the Operations, writes
down what happens to every machine: INFO (a machine switched on or off), WARN (a degraded power supply, or a
redundant one that lost a unit) and ERROR (two Mainframes on one network, a machine that went dark because no energy
reached it, or because its power supply failed; these added 2026-11-08; a machine that went dark on unstable power,
and what the Operations of a machine that went dark were carrying, how much and of what, also added 2026-11-08).
It filters by level, machine and period,
prints, and exports to a text file on a disk of the Mainframe. A network keeps its last seven days, at most 2,000
events. When the team that owns the network (the team of its
Mainframe's operator) reaches an era of hardware, or one of J's Industrial's tiers, the log says so in an INFO line
("Era Standard reached by" the team); a team reaches an era the first time one of its players assembles a working
computer of it (added 2026-11-08).

## Wi-Fi

*Added 2026-11-09.*

From the Legacy on, a small computer can join the network with no cable of its own: a **Wi-Fi card** in it
([Hardware](HARDWARE.md#cards)) and a **Wireless Access Point** on the network within its reach. The access point is a
network device: it takes the access cable of its era or an older one and the energy cable, both through one face, and
gives the network it is cabled to to every computer with a card round it. Each computer joins the nearest access point
that reaches it; several in one place do not conflict, and one takes any number of computers. A computer with a cable
keeps its cable for its link. Aboard a ship of Create Aeronautics, the reach is measured from where the ship flies
now, so a ship talks to the ground by Wi-Fi (added 2026-11-11, [Create](CREATE.md)).

| Access point | Standard | Reach | Speed | Draw | Where it goes |
| --- | --- | ---: | ---: | ---: | --- |
| Legacy | 802.11b/g | 24 blocks | 4 it/t | 10 W | on a desk, its cables from behind |
| Transition | 802.11n | 32 blocks | 16 it/t | 12 W | on a desk, its cables from behind |
| Standard | 802.11ac | 48 blocks | 64 it/t | 15 W | on a ceiling or a wall, its cables through that face |
| Advanced | Wi-Fi 6 | 64 blocks | 128 it/t | 20 W | on a ceiling or a wall, its cables through that face |

A link goes at the speed of the older of its two ends, the card or the access point, as real Wi-Fi does, and copies
of files and songs to and from a computer on Wi-Fi go no faster. An access point fixed to a ceiling or a wall comes
off with the block it is fixed to. The Network Manager lists each access point as an ACCESS POINT node on its cable,
and shows a computer's link as Wi-Fi with its speed; the line under the Devices list says which end sets it for the
computer picked. The numbers are estimates.

### Portables

*Added 2026-11-09.*

The **portables** are computers a player carries: a PDA in the Legacy and the Transition, a smartphone from the
Transition on, a tablet from the Standard on, each with the hardware of its era and its system from the factory
(computers/docs/design/wireless.md). A portable carried where an access point reaches its player joins that network at
the speed of the older end, its own Wi-Fi or the access point's, and the Network Manager lists it as a PDA, SMARTPHONE
or TABLET node on Wi-Fi.

| Device | Era | System | Chip | Memory | Storage |
| --- | --- | --- | --- | ---: | ---: |
| Hightide Pocket | Legacy | Frames Pocket | 400 MHz | 64 MB | 64 MB |
| Hightide Pocket Pro | Transition | Frames Pocket | 624 MHz | 128 MB | 256 MB + a 2 GB card |
| qPhone | Transition | qOS | 412 MHz | 128 MB | 8 GB |
| Motoroller Landmark | Transition | Droidix | 550 MHz | 256 MB | 16 GB |
| qPhone 5 | Standard | qOS | 2 x 1.3 GHz | 1 GB | 32 GB |
| Hightide Wave | Standard | Droidix | 4 x 2.3 GHz | 2 GB | 32 GB |
| qPad Air | Standard | qOS | 3 x 1.5 GHz | 2 GB | 64 GB |
| Sunsung Nebula Tab | Standard | Droidix | 2 x 1 GHz | 1 GB | 16 GB |
| qPhone X | Advanced | qOS | 6 x 2.4 GHz | 3 GB | 64 GB |
| Sunsung Nebula S10 | Advanced | Droidix | 8 x 2.7 GHz | 8 GB | 128 GB |
| qPad Pro | Advanced | qOS | Quince Q1, 8 x 3.2 GHz | 8 GB | 256 GB |
| Sunsung Nebula Tab S7 | Advanced | Droidix | 8 x 3.1 GHz | 6 GB | 128 GB |

Used, a portable is held up in the middle of the view over the blurred world, its own face framing its screen, and its
screen and buttons are pressed with the mouse; the right button goes back. Its system is the one of its years:
Frames Pocket of 2002 and 2007, the qOS of 2007, 2013 and 2017, Droidix of 2009, 2014 and 2019, the tablets in their
family's phone look. It starts with its maker's mark, then its system's start, then its lock, which lets go with its
slider, a swipe up or its Unlock key. It shows the world's time and the day as Day N; the network's alerts wait in each
system's place (Frames Pocket's balloon and Today, the qOS's lock and Notification Center, Droidix's lock and shade),
counted by a badge on the Alerts icon; its settings show its hardware, its Wi-Fi and its battery. Its apps come from its
family's store (the Frames Marketplace, the Quince Store, the Droidix Store), served by the network's Mirror, everything
free: the network app and the alerts come with every portable, the camera, the gallery and Maps with the smartphones and
the tablets; the Crypto Wallet installs from the Transition on, the Messenger and prospecting from the Standard on.

A portable carried runs on its battery, the bar under its icon: a full charge lasts about two game days on a smartphone
or a tablet and a day and a half on a PDA; flat, it switches off. The **Mobile Device Charging Dock** of its era holds
one by a right click and gives it back to an empty hand; it takes its energy from behind and fills the battery at its
charger's watts (5 W on the Legacy and the Transition, 10 W on the Standard, 15 W on the Advanced's wireless stand),
taking from its line only what it gives the battery. A portable in its dock is off, its screen showing the battery
filling, the dock's lamp lit. What a portable keeps (its apps and its alerts) lives on the server, by the id its item
carries. The numbers are estimates.

Each family draws its apps in its own look. The **network app** is a smaller Network Interactor with three tabs:
Stock, the network's items, searched, each opening to be requested; Device, the portable's own storage; and Craft,
what the network makes, how many, its plan and the jobs running on it. A request puts the items into the portable's
own storage, never straight into the inventory; Device takes them into the inventory (Take) or back to the network
(Return), and its Deposit lists the player's inventory row by row, each given to the network or all at once. A
portable holds what its storage holds over the item size of its era's disk, less its system: 4 items on the Hightide
Pocket, 8 on the Pocket Pro with its card, 112 on the qPhone 5, 480 on the Nebula S10, 1,000 on the qPad Pro. On
Frames Pocket the left soft key requests the item picked and the Menu holds the other pages, as the Pocket PC's
programs did.

A player who carries a portable joined to a network also builds from that network's storage: J's Core's building
tools take from it when the inventory has none of a block, and its Multiblock Builder takes from it when **Network
storage** is the source chosen on its display. A block that cannot stay goes back to the network, or to the player
when the network has no room.

The network sends **alerts** to the portables on it: an item below the level a portable watches it at, an Operation
that failed, a degraded power supply, a UPS on its battery (a line in the network's log too), and a network conflict.
The **Alerts** app turns the five kinds on or off for its portable and keeps up to 12 watched items, each with its
level, halved or doubled; a watched item is told once when it goes below its level, and again only after it came back
up. A rule for the whole network belongs to the automation. A player's client setting
(`jscomputers-client.toml`, `notifications_on_hud`) shows the alerts on the HUD too, as J's Core notifications at the
attention level, naming the device they came from.

The **camera** takes what the player sees, without the hand or the HUD, cut to four by three at the resolution of the
portable's era (640 x 480, 1280 x 960, 4032 x 3024), and keeps it in the **gallery** as a PNG the server's store
keeps, in the portable's own storage by its bytes, up to 60 pictures. From the gallery a picture goes to a personal
computer of the network as a PNG file in its pictures folder (`Users/Public/Pictures`, or `~/Pictures` on a Unix),
naming the picture the store keeps, as a song's file does. **Maps** draws J's Core's map round the player with the
network's Mainframe, its access points and their reach, and the Core's markers; the wheel zooms it. **Prospecting**
shows the veins within 64 blocks as a compass and bars, from J's Core's world kit: which way each lies and how
strongly, never where. The **Messenger** talks to the network's Messenger service, its rooms and its people the same
as a computer's.

Whoever loses a portable loses what was on it: when its item is destroyed (lava, fire, the void, despawning) the server
lets its state go, and a portable out of every inventory and every dock for 72 hours of the server running is let go
too (`[wireless] lost_device_hours` in `jscomputers-server.toml`), unless it was last in the inventory of a player who
is away; a portable holding a wallet is never let go.

## Two places over a long-distance line

*Added 2026-11-09.*

A long-distance line runs between the ports of two WAN Gateway Computers ([Hardware](HARDWARE.md)). It is up while
both gateways run **Nouvell BorderLink**, their service, with both ports on and each card taking the line. What it
does depends on where the Mainframes are, which each gateway reads off its own place, never across the line:

- **Extension**: a Mainframe on one side only. The network reaches over the line, and the other place's computers,
  racks and cables join it as if one cable ran between them. Taking the line down cuts the far place off again.
- **Federation**: a Mainframe on each side. The two stay two networks, each with its own storage, index and
  Operations, and each works with the other as far as the other allows. A Mainframe put on the far side of an
  extension makes a federation, never a network conflict.

Each gateway's port decides what the network at its far end may do here: **see** its items, **take** items, **send**
items and **craft** there; a new port lets it see and nothing more. **Relay** lets the networks behind the far side
reach through this one, up to three lines away; a way through other networks runs at its slowest line.

The Network Interactor's **Federated** tab shows a federated network in a view of its own: take its items into the
computer, drop a stack on its grid to send it, or have it craft something, which stays there. Each is an Operation
that crosses: it runs on both networks, each side logs it, its line holds what is on its way, and cancelling it here
cancels the other half too. While the far side is not loaded it waits for its chunks; when the line goes down, what is
on it is lost if `item_loss.network_split` is on (the default), and otherwise the Operation waits for the line.

BorderLink shows the four ports, the line, the far end, the mode and the state of each, and the switches of the port
picked; on MC-NET it is its own operating space, with Ports, Log and Console, and at the prompt `borderlink` lists the
ports, shows one (`borderlink port 2`) and sets its switches (`borderlink port 2 relay on`). Its Log is the gateway's
own lines of the network's log: a line coming up, and with what, or going down.

## Storage: items are data

*Added 2026-06-05.*

The network keeps items, liquids and every other state of matter of J's Core (gases, plasmas, supercritical
fluids, slurries and exotic matter) as data on disks, all counted the same way (a bucket weighs as much as an item);
a chemical mod's chemicals J's Core has no state for are kept as chemicals, and the ones it links to its materials
(Mekanism's hydrogen, oxygen, chlorine, deuterium and tritium) as those gases. How much room an item takes depends on
the disk's era: 1 MB in the Vintage, 16 MB in the Legacy, 256 MB from the Transition on. Disks hold folders, files and
programs as well.

The network's storage is the disks of the **Servers** in its racks. An **External Storage Bus** on a cable lets it
use a chest beside it as storage too, ten times slower than a server. **Import** and **Export Buses** bring items
in from an inventory and send them out to one ([Cables](CABLES.md#buses)).

A server running the **Data Compressor** (Vaultis, Standard; added 2026-11-11) fits a fifth more items on its
drives, and every read from it waits two ticks longer: worth it on a hard disk, a poor trade on NVMe. The network
keeps a **book of losses** of the items that stop existing, which a backup restores from
([Systems and programs](SYSTEMS.md#backups)).

**Datacenter sections** (added 2026-11-08). The racks behind one face of a **Server Router** are a section, which
the **Cluster Manager** runs as one machine ([Hardware](HARDWARE.md#servers-and-their-racks)). Its **Diagnostics** tab
shows, over the last minute, the section's latency (the ticks from a request entering the router to its server's
answer) against its base (what the servers' drives take with nothing in the way), its throughput in items a second
against the most it can move, and how many alerts stand; the last hour, minute by minute, with the latency and the
throughput each on its own scale; and the alerts standing and since when. A section's alerts are a server's disks
over 90% full (WARN) or 98% (ERROR), a rack over its thermal budget, the router at its rack budget, and the latency
over twice the base (WARN, while it lasts). Each goes into the network's event log as it is raised. The router keeps
the hour with itself, across restarts.

## Operations

*Added 2026-06-05.*

Everything the network does for you is an **Operation**: a request it queues, gives to a queue, works over some
ticks and finishes. They work like the commands of a database, which is where their names come from:

| Operation | What it does |
| --- | --- |
| SELECT | Takes items out of storage, to the computer that asked. |
| INSERT | Puts items into storage. |
| MOVE | Takes items out to a place. |
| DELETE | Sends items out of the network into an outside inventory (it destroys nothing). |
| CRAFT | Makes items, from crafting table recipes, machine recipes, or both in many steps ([Autocrafting](AUTOCRAFTING.md)). |
| UPDATE | Changes stored items with the workshop cards: smelts, enchants, repairs, combines, names them. |
| ANALYZE, REINDEX, VACUUM | Look after the network's index of what it stores. |
| DROP | Destroys what is stored, with no question, as on a real database. |

An Operation is always in one of eight states:

| State | Means |
| --- | --- |
| PENDING | Waiting in its queue. |
| PROCESSING | Being worked. |
| WAITING | Paused for something: items another Operation holds, a busy machine. |
| COMPLETED | Done. |
| COMPLETED_PARTIAL | Done with less than was asked (not enough items). |
| FAILED | Could not be done, and says why. |
| RESOURCE_LOCKED | Could not be done because what it needed is held. |
| DISCARDED | Dropped before it finished. |

Each has a **priority** (low to high; medium unless you say). A request that waits long climbs in priority so that
nothing waits forever, and one that waits on something for too long gives up. The Mainframe's **Task Manager**
shows the queue as the network works through it.

## IQL, the network's language

*Added 2026-08-26.*

**IQL** (Item Query Language) is how you ask the network for anything in words, at a computer's command prompt, in
a program, or in a script. It reads like the language of databases:

```
SELECT 64 iron_ingot
CRAFT 64 torch
QUERY items WHERE qty > 100
QUERY items ORDER BY name LIMIT 2
QUERY disks
```

- **Requests**: `SELECT`, `INSERT`, `DELETE`, `MOVE`, `DROP`, `CRAFT`, `COUNT`, `LOCK`, `UNLOCK`, each with a
  quantity and an item, and optionally `FROM`, `TO`, `WHERE`, `IF`, `ORDER BY`, `LIMIT` and `PRIORITY` (`LOW`,
  `MEDIUM_LOW`, `MEDIUM`, `MEDIUM_HIGH`, `HIGH`).
- **Questions**: `QUERY` or `SHOW` and what to look at (`items`, `servers`, `disks`, `operations`, `recipes`,
  `computers`, `events`, `power`), with `WHERE`, `ORDER BY`, `LIMIT`. Other mods' machines on the network are read the same
  way, by their table's id or, when no other table shares it, its last part (`QUERY mymod:reactors WHERE temperature
  > 90`): a row for each machine, its columns with their units, and `at`, where it stands (added 2026-10-25).
- **World events** (added 2026-10-25): `QUERY events` lists those the network sees, under way where it stands, and
  coming when a machine on it is the event's instrument; each step goes into the network's event log as well.
- **The power of the racks' bays** (added 2026-11-08): `QUERY power` lists every server of a rack with a PDU, the
  rack unit it tops (`row`), what it draws from the line (`watts`) and whether its bay is switched on (`on`, yes or
  no). `UPDATE power SET on = no WHERE server = 'VAULT-01'` switches the bays the WHERE keeps off, `SET on = yes` on,
  as their PWR switches would; from a job it turns servers on and off by rule. The reply counts the bays switched,
  and those that did not start for want of energy or with a failed supply ([Hardware](HARDWARE.md#upses)).
- **Create's kinetic networks** (added 2026-11-11): with Create and J's Industrial installed, `QUERY create_kinetics`
  lists the Create blocks the network's computers read through the industrial control cable, with their speed, stress,
  capacity and whether they are overstressed, and `UPDATE create_kinetics SET target_speed = 64` aims the Rotation
  Speed Controllers its WHERE keeps at that speed ([Create](CREATE.md)).
- **Upkeep**: `ANALYZE`, `VACUUM`, `REINDEX`.
- **Changing items**: `UPDATE ... SET SMELT`, `ENCHANT`, `REPAIR`, `COMBINE ... WITH ...`, `NAME '...'`. The work
  is the asking Personal Computer's workshop cards', at their price (added 2026-11-08): by default energy, which
  that computer owes and pays off as the grid fills its store, the next paid work waiting until it has
  ([Hardware](HARDWARE.md#cards)); the server can make it levels instead, both, or nothing (`update.cost`).
- **Other mods' machines** (added 2026-10-25): `RUN` and the id of a kind of Operation another mod registered, to
  have one of its machines on the network do it: `RUN mymod:accel_run ON 'Cyclotron-3fa2c1' WITH beam = 'proton',
  energy = 20 PRIORITY HIGH`. `ON` names the machine by the name you gave it on its screen, by its block's name and
  short id, or by where it stands (`ON '12, 64, -3'`, as the `at` column writes it); without `ON`, the able machine
  with the fewest such requests in flight takes it. `WITH` gives the fields the kind reads, by name; one it has not,
  or a value it cannot read, is refused, naming the field. The request waits for a queue by its priority as any
  other, goes to the machine, and lets the queue go: the machine's work, however long, holds none of the
  Mainframe's queues. It stays in the list of what runs (where you can call it off, which stops the machine, or
  change its priority) and goes into the log once the machine says it is done, under `RUN`, with its kind, the
  machine and what was asked. A machine that waits on items for its work and says which (added 2026-10-30), as a
  crane over a bay waits for the blocks of its plan after `RUN jscore:assemble WITH plan = 'Orbital I'`, is
  brought them: once a second the network sends it the first item it needs that the disks hold, an export in the log,
  and has the ones it lacks crafted where its patterns make them.
- **Settings**: `SET BUS 'name' ...`, `SET REDSTONE 'name' ...`, `SET INTERFACE ...`, `SET ROUTER ...`.
- **Saved work** (engines that offer it): `CREATE VIEW`, `CREATE PROCEDURE` and `EXEC`, and `CREATE JOB` with
  `EVERY` a time or `WHEN` a condition, for the network to run on its own. A job can wait for a world event:
  `CREATE JOB safe AS EXEC shutdown WHEN EVENT mymod:solar_storm COMING` (or `UNDER WAY`, or `ENDED`), and answer it
  on another mod's machine: `CREATE JOB shield AS RUN mymod:shield_up WHEN EVENT mymod:solar_storm COMING`. A job
  can also wait for the network's team to reach an era or a tier: `CREATE JOB retool AS EXEC tooling WHEN ERA
  standard REACHED`, or `WHEN TIER t3 REACHED`; it fires once, as the team gets there, if the Automation Engine is
  running then.
- A script is statements separated by `;`, with `--` comments, and can ask for values when it is run:
  `CRAFT <count, number, 64> <item, name, torch>`.

A Σ# program speaks IQL too ([Σ#](SIGMA.md)).

## Engines

*Added 2026-09-23.*

What reads IQL and plans the work is the Mainframe's **engine**, a program installed on it. Each speaks its own
version of the language and offers its own extras:

| Engine | From | What it adds |
| --- | --- | --- |
| Midsoft IQL Server | every Mainframe | IQL, with views and procedures. Versions 4.2 (Vintage), 2000, 2008, 2012, 2022 (Advanced). |
| NextgreIQL | Legacy | Shows how it plans (EXPLAIN), takes hints after a statement (`PREFER COMPUTER 'Bench A'`), and planner rules from other mods. |
| Prophet YourIQL | Legacy | You declare a state (`KEEP steel_ingot >= 10000`) and it works out the Operations that reach it and keep it. |

Programs written for one engine (the Nextgre Planner Studio, the Prophet Reactive Console) say so when opened on a
network that runs another.

## Server settings

The settings of the Operations engine are the series' own, in `jstech-operations.toml` (in the world's
`serverconfig` folder; also on the settings screen): how long an Operation waits before it gives up, how fast a
waiting one climbs in priority, how long a saved one nobody resumed is kept. J's Computers' own are in
`jscomputers-server.toml`:

| Setting | Default | What it does |
| --- | --- | --- |
| `balance.hdd_latency_ticks`, `ssd_latency_ticks`, `nvme_latency_ticks` | 10, 3, 1 | How long each kind of disk waits before a transfer starts. |
| `balance.subframe_efficiency_factor` | 0.6 | The share of its capacity a Subframe lends its Mainframe. |
| `balance.program_machine_micros` | 1000 | Real time, in microseconds, one machine may spend running its programs in a tick. |
| `balance.program_server_micros` | 8000 | The same for every machine of the server together. |
| `boot.show_boot_menu` | on | Stop at the boot manager when a computer starts. |
| `hardware.psu_failures` | on | Power supplies wear out and fail ([Hardware](HARDWARE.md#power-supplies-psu)). |
| `hardware.psu_life_multiplier` | 1.0 | How much longer every power supply lasts, from 0.1 to 10. |
| `install_by_hand.arch_every_step` | off | Ask for every step of Arch Linux's guide. |
| `item_loss.power_loss` | on | What the Operations were carrying is lost when the machine running them goes dark suddenly ([Hardware](HARDWARE.md#power-supplies-psu)); off, it is given back. |
| `item_loss.network_split` | on | What an Operation was carrying is lost when the network splits under it, a cable pulled between the Mainframe and a server; off, it is given back. |
| `item_loss.network_conflict` | on | A network conflict loses what the discarded Operations were carrying and corrupts a tenth of what both networks store; off, it costs nothing. |
| `item_loss.corruption` | on | Storage can be corrupted at all, by a conflict or a thermal trip ([Hardware](HARDWARE.md#corruption-and-the-defrag)); off, nothing is. |
| `install_by_hand.gentoo_every_step` | off | Ask for every step of Gentoo's handbook. |
| `power.draw_energy` | on | Computers draw energy through their power supplies ([Hardware](HARDWARE.md#power-supplies-psu)); off, they run free. |
| `prompt.list_commands` | off | The `listcmd` command on every system. |
| `programs.outside_components` | off | Let programs show components that reach outside the game. |
| `soundfoundry.catalog` | on | Offer the server's music catalogue ([Music on a server](SOUNDFOUNDRY.md)). |
| `soundfoundry.ethernet_kilobytes_per_second` | 512 | How fast songs travel over Ethernet. |
| `soundfoundry.hbw_kilobytes_per_second` | 2048 | The same over HBW. |
| `soundfoundry.hpc_kilobytes_per_second` | 8192 | The same over the high compute cable. |
| `update.cost` | `energy` | What the workshop cards' work costs, an UPDATE's and the Workshop's: `energy`, `xp`, `both` or `none`. |
| `teracoin.target_minutes` | 10 | A Teracoin block's target time ([Systems](SYSTEMS.md#teracoin)). |
| `teracoin.reward`, `halving`, `halving_blocks` | 50, on, 2100 | What a block pays, in TRC, and whether and how often it halves. |
| `teracoin.floor_days` | 100 | The difficulty's floor grows by its start every this many world days. |
| `teracoin.rare_item_chance`, `rare_items` | 0.05, five items | The share of blocks that bring a rare item, and which. |
| `teracoin.printer.prices` | twelve items | What the 3D printers print and its price in TRC, one `item=price` a line. |

Each player's own (`jscomputers-client.toml`): `client.desktop_cursors` (each system's own pointer) and
`client.outside_components`. Desktops act at once, without animation, for a player who turns on `motion.reduce` in
J's Core's `jstech-accessibility.toml`, as every motion of the series does.

## What can go wrong

- **An Operation stays PENDING.** The Mainframe is busy, off, or has no processor time to spare. Watch the Task
  Manager; a faster Mainframe or more graphics cards (more queues) help.
- **It ends RESOURCE_LOCKED.** What it needed is held by another Operation or a `LOCK`. Wait, or `UNLOCK`.
- **It ends COMPLETED_PARTIAL.** There was less than you asked for.
- **Storage is full.** Add disks to the servers, or servers to the racks; a newer era's disks hold more.
- **Two Mainframes, nothing works.** Each network has one Mainframe. Split the cables.
- **An IQL line is refused.** The prompt says where it stopped reading. Saved views, procedures and jobs need an
  engine that offers them.
