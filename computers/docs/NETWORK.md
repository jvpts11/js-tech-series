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

## Storage: items are data

*Added 2026-06-05.*

The network keeps items, fluids and chemicals as data on disks, all counted the same way (a bucket weighs as much
as an item). How much room an item takes depends on the disk's era: 1 MB in the Vintage, 16 MB in the Legacy,
256 MB from the Transition on. Disks hold folders, files and programs as well.

The network's storage is the disks of the **Servers** in its racks. An **External Storage Bus** on a cable lets it
use a chest beside it as storage too, ten times slower than a server. **Import** and **Export Buses** bring items
in from an inventory and send them out to one ([Cables](CABLES.md#buses)).

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
| DROP | Destroys what is stored. |

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
- **Questions**: `QUERY` or `SHOW` and what to look at (`items`, `disks`...), with `WHERE`, `ORDER BY`, `LIMIT`.
- **Upkeep**: `ANALYZE`, `VACUUM`, `REINDEX`.
- **Changing items**: `UPDATE ... SET SMELT`, `ENCHANT`, `REPAIR`, `COMBINE ... WITH ...`, `NAME '...'`.
- **Settings**: `SET BUS 'name' ...`, `SET REDSTONE 'name' ...`, `SET INTERFACE ...`, `SET ROUTER ...`.
- **Saved work** (engines that offer it): `CREATE VIEW`, `CREATE PROCEDURE` and `EXEC`, and `CREATE JOB` with
  `EVERY` a time or `WHEN` a condition, for the network to run on its own.
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

The settings of Operations are in J's Core's server settings file (`jstech-balance.toml`, in the world's
`serverconfig` folder; also on the settings screen): how long an Operation waits before it gives up, how fast a
waiting one climbs in priority, how long a disk waits before a transfer, how much of its capacity a Subframe lends.
J's Computers' own are in `jscomputers-server.toml`:

| Setting | Default | What it does |
| --- | --- | --- |
| `boot.show_boot_menu` | on | Stop at the boot manager when a computer starts. |
| `install_by_hand.arch_every_step` | off | Ask for every step of Arch Linux's guide. |
| `install_by_hand.gentoo_every_step` | off | Ask for every step of Gentoo's handbook. |
| `prompt.list_commands` | off | The `listcmd` command on every system. |
| `programs.outside_components` | off | Let programs show components that reach outside the game. |
| `soundfoundry.catalog` | on | Offer the server's music catalogue ([Music on a server](SOUNDFOUNDRY.md)). |
| `soundfoundry.ethernet_kilobytes_per_second` | 512 | How fast songs travel over Ethernet. |
| `soundfoundry.hbw_kilobytes_per_second` | 2048 | The same over HBW. |
| `soundfoundry.hpc_kilobytes_per_second` | 8192 | The same over the high compute cable. |

Each player's own (`jscomputers-client.toml`): `client.reduce_motion` (desktops act at once, without animation),
`client.desktop_cursors` (each system's own pointer) and `client.outside_components`.

## What can go wrong

- **An Operation stays PENDING.** The Mainframe is busy, off, or has no processor time to spare. Watch the Task
  Manager; a faster Mainframe or more graphics cards (more queues) help.
- **It ends RESOURCE_LOCKED.** What it needed is held by another Operation or a `LOCK`. Wait, or `UNLOCK`.
- **It ends COMPLETED_PARTIAL.** There was less than you asked for.
- **Storage is full.** Add disks to the servers, or servers to the racks; a newer era's disks hold more.
- **Two Mainframes, nothing works.** Each network has one Mainframe. Split the cables.
- **An IQL line is refused.** The prompt says where it stopped reading. Saved views, procedures and jobs need an
  engine that offers them.
