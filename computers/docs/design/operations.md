# Operations

Everything the network does is an **Operation**. The network is treated as a distributed database: every interaction can
be traced, audited, and runs on its own time. An Operation says **what** to do; **how** is left to SubOperations, run
locally by each computer involved.

## What an Operation is

J's Core gives the framework: an **Operation type** (an id `namespace:path`, the class of its arguments, a category, a
minimum industrial tier, the network categories that may run it, and what handles it) in a registry of types that closes
at the end of loading (each mod registers its own), plus lifecycle events on J's Core's event bus (created, started,
completed, failed, discarded, with the network, the id and the type).

In J's Computers, an Operation under way has an id, a type, a priority, a step per tick with the throughput budget it
was given, `abandon()` (let go of what it holds and give it back), `cancel()` (cancelled by the player) and its
**record**:

- its type, its key (an item, a fluid or a chemical), the amount asked for and the amount moved;
- its status and its priority;
- its source lines (which server gave each part), its SubOperations, the ticks it waited and ran;
- the cause of a failure (translatable text with its values).

Every Operation keeps **who** asked for it (the player's id; a machine is nobody; a Σ# program acts for its operator)
and its **provenance**, with the means and the machine: `lab-pc (Interactor)`, `Import Bus "name"`,
`host (Σ#: Ns.Class)`, `IQL`, `host (CC #id)`. Persistent Operations are saved on the Mainframe.

### Status

Eight states, with stable ids from 1 to 8 (0 is unknown). Three are active and five are final:

| Status | Meaning |
| --- | --- |
| PENDING | created, waiting for a queue on the Mainframe |
| PROCESSING | running |
| WAITING | waiting for something: items held by another Operation, a busy machine (times out after 1,200 ticks by default) |
| COMPLETED | done |
| COMPLETED_PARTIAL | done with less than was asked (a partial craft) |
| FAILED | failed, and says why |
| RESOURCE_LOCKED | failed because WAITING timed out |
| DISCARDED | dropped before it finished: the Mainframe shut down, lost its system disk or went into conflict, or the player cancelled it |

The WAITING timeout is 1,200 ticks (60 seconds) by default, at least 1, set per server in `jstech-balance.toml`
(`operation_waiting_timeout_ticks`).

### Priority

LOW, MEDIUM_LOW, MEDIUM (the default), MEDIUM_HIGH and HIGH. A player's manual Operations are MEDIUM by default, the
same as automation. The player chooses the priority in the interface and can change it later (the Task Manager, Σ#).

**Aging:** an Operation that is ready but has no queue goes up one level every 600 ticks (the `priority_aging_ticks`
key; 0 turns it off), up to HIGH, so nothing waits forever. Ties keep the order of arrival.

### Queues

The Mainframe has 1 queue for its processors plus 1 per graphics card (plus the ones its Subframes lend). Each Operation
that gets a queue runs at **that** queue's speed ([Hardware](hardware.md)): queues don't share capacity, and the fastest
queues go to the Operations served first. A line of waiting Operations only exists when there are more Operations than
queues: the rest wait in PENDING. The practical limit is how many cards the Mainframe counts (6) and what they draw.

What happens to Operations when the Mainframe shuts down is in [Power](power.md); in a network conflict, in
[The network](network.md).

## The catalogue

J's Computers registers these types (namespace `jsc`): `select`, `insert`, `move`, `delete`, `drop`, `update`, `craft`,
`processing`, `multi_stage`, `analyze`, `reindex`, `vacuum`, `lock` and `unlock`. SELECT, INSERT, MOVE, DELETE, UPDATE
and the crafting ones are **timed** (accepted in PENDING and worked over ticks); ANALYZE, REINDEX, VACUUM, DROP, LOCK
and UNLOCK are **instant**. QUERY is not an Operation: it is a question IQL asks the index.

### Storage

Items, fluids and chemicals are handled the same way: an Operation's key is an item, a fluid or a chemical.

**SELECT** (key, amount, destination) takes things out of the network to a destination that receives data: the local
storage of the computer that asked ([Storage](storage.md)), a terminal, a bus, a container. It never goes straight into
the player's own inventory. It reads first from the places with the shortest wait. By hand, the player asks for an item
in the Network Interactor with an amount and a priority; automatically, programs (Σ#, IQL, Automation) and ComputerCraft
do.

**INSERT** (key, amount, source) puts things into the network. By hand, the player hands it over at the computer or in
the Interactor (the window shows the player's real inventory; shift-click inserts), at the priority asked (MEDIUM by
default). Automatically, the Import Bus keeps an internal buffer and flushes it in batches ([Storage](storage.md)); if
the network is full, the bus holds on and keeps gathering.

**Handing things over by hand follows one rule:** a left click or a shift-click stores the item as it is (a full bucket
goes in as a bucket); a right click with a full container hands over its contents; a right click with an empty container
fills it from the fluid or chemical input (a bucket only with 1,000 mB).

**DELETE** (key, amount, external destination) takes items out of the network to an outside inventory, the one the
Export Bus faces. It destroys nothing.

**DROP** (key, server or everything) **destroys** what is stored: one type (on the servers where that is allowed), one
server, or the whole network. It can't be undone. DROP doesn't drop items on the floor: it destroys them, for good.
Players who know SQL will understand at once what it does; the others will learn it once, probably in a way they will
remember. Who may run a DROP is decided with security ([Security](security.md)).

**MOVE** (key, amount, source, destination) takes items from where they are on the network to a place. Every part
records which server it came from. The items an Operation holds are out of the available count until it finishes.

**UPDATE** (key, action, amount [, with item]) is the network's door to the **personal-use cards** (the Crafting Table,
Furnace, Enchanting and Anvil Cards, [Peripherals](peripherals.md)) of the computer that asks. Its actions are SMELT,
ENCHANT (OFFER 1, 2 or 3), REPAIR, COMBINE (with another item) and NAME.

1. The UPDATE is issued by the Personal Computer that has the cards.
2. SUB_SELECT: the item leaves its server for a drawer no query sees.
3. SUB_UPDATE: the card applies the action to the item, by the Workshop's rule and price.
4. SUB_INSERT: the changed item goes back to its server, if it fits.

The one asking has to be present, and only the cards of the computer that asks will do, so an IQL job (which runs as the
Mainframe) can't. **No craft ever uses the personal-use cards:** CRAFT Operations never use them as part of an automated
recipe. In IQL: `UPDATE [amount] item [FROM server] SET action [args] [WITH item] [WHERE ...]`; in Σ#,
`Operations.Update`.

### Queries and the index

**QUERY** (an IQL question, not an Operation) reads the network's index, which lives in the Mainframe's memory. No disk
reads, no locks. QUERY or SHOW and what to look at (items, disks...), with WHERE, ORDER BY and LIMIT; COUNT. It is what
the Interactor's search and the programs use.

**ANALYZE** reconciles the index with the real state of the disks. It is **incremental**: it only reads again what
changed since the last one (every store counts its own changes). Instant. On NextgreIQL, it also gathers the planner's
statistics. ANALYZE is about the index; gathering data for the AI is SAMPLE ([Artificial intelligence](ai.md)).

**REINDEX** rebuilds the index from scratch. The rebuild runs on a virtual thread, without stopping the tick: reads see
the old catalogue until the swap, and the change count makes sure the next incremental pass misses no write. The network
does **not** go offline.

**VACUUM** frees the "ghost entries": index entries that point to items no longer on the disk. It deletes no real items.

**The index's health is visible**, in the Upkeep header of the Mainframe's terminal and in the ISMS explorer:

- **OK** (nothing highlighted): the index is consistent.
- **STALE** (yellow): hot removals and dirty events (taking a disk out of a rack bay with its server on, for example)
  left unconfirmed entries. It lists the item types affected and recommends REINDEX.
- **FRAGMENTED** (orange): mass removals left ghost entries. It recommends VACUUM. Ghost entries are listed before stale
  ones.

Every entry gives the item type, its source and how serious it is. This is the interface side of looking after the
network: the racks' hot-swap makes reindexing and vacuuming part of an operator's day.

### Concurrency: LOCK and UNLOCK

- **Automatic:** every Operation that uses items holds them (a table of locks by Operation, key and server) and lets
  them go when it finishes, fails or is discarded. Held items vanish from the available count (in the Interactor and in
  QUERY answers). A second Operation that needs them goes into WAITING, with the timeout. QUERY and LOCK are atomic:
  there is no race between reading the index and holding items.
- **By hand:** LOCK and UNLOCK asked for by a player or a program, instant, saved and restored with the Mainframe (IQL
  `LOCK` and `UNLOCK`; `interac lock`, `unlock` and `locks`). Today any player with access to the prompt, to IQL or to
  the Interactor can lock and unlock. Who may unlock by hand will be decided in the directory ([Security](security.md)).

### Crafting

**CRAFT** (key, amount, partial?, priority?) asks the network engine to make items: bench recipes, machine recipes, or
both in several stages. If only part can be made: COMPLETED_PARTIAL. **PROCESSING** is one machine recipe, at a Crafting
Interface. **MULTI_STAGE** is a pipeline of stages, which finds its stages by id. All of it is in
[Autocrafting](autocrafting.md).

### Choosing items

There are three ways to choose items: IQL's WHERE (it picks the variants of an item: a damaged tool, a name; with IF,
ORDER BY and LIMIT), the buses' filters (up to 5 items, only-these or all-but, amounts to keep and at most, by tag and
by loose matching in the Advanced), and the buses' conditions (stock under N of an item or a #tag, the time of day,
after another bus). There is no universal filter: each way stays at the level of whoever uses it, the bus in its window
and by its era, IQL and Σ# for whoever wants full expressions.

### Other mods' Operations

There is no closed list of types: every mod registers its own in J's Core's registry (an id `namespace:path`, a
category, a minimum tier, who may run it). The categories already reserved are MILITARY, INDUSTRIAL, SPACE, TRANSPORT,
AGRICULTURE, GEOLOGICAL, OCEANIC, CONSTRUCTION, ROBOTICS and SECURITY. A machine of another mod is driven through J's
Core's capabilities; J's Computers never depends on the mod that brings it. Each mod's Operations are described by that
mod, which owns them; this design only describes the registry, the categories, the dispatch and the capabilities. The
AI's and the simulator's go with those systems.

## How an Operation runs

1. **It is received and queued.** All demand for work (the Interactor, terminals, buses, the Network Gateway, craft
   dialogs, programs, IQL) comes in through the **network's Operations service** and goes to the network engine the
   Mainframe runs. The Operation is queued with its priority.
2. **It is split into SubOperations.** The Mainframe looks at the index (in memory). For Operations that use items, it
   holds them before dispatching. It splits the Operation into parts, one per store involved. A SELECT of 1,000
   cobblestone with items on 2 servers becomes SUB_SELECT-1 (Server A sends 600 to PC-Steve) and SUB_SELECT-2 (Server B
   sends 400 to PC-Steve).
3. **It is dispatched with a budget.** The Mainframe's scheduler hands out queues by effective priority and feeds each
   Operation, every tick, a **throughput budget**: the smallest of its queue's speed, the memory buffer, the slowest
   cable to the server, the server's processor and memory capacity, and the disk.
4. **It runs locally.** Each store reads at its disk's wait (HDD 10, SSD 3, NVMe 1 ticks, configurable; the SSD moves 4
   times and NVMe 16 times faster than the HDD) and its memory's ([Hardware](hardware.md)). The waits are virtual
   threads (Java 21), one per disk, in parallel, which resume on a tick; everything that touches the world runs on the
   main thread.
5. **It progresses and finishes.** The Mainframe adds up the parts' progress. When they are all done, the Operation is
   COMPLETED (or COMPLETED_PARTIAL, or FAILED with the cause) and the locks are released.

SubOperation types in the log: SUB_SELECT, SUB_INSERT and SUB_UPDATE, with the line states SUB_READING and STREAMING.
The machine stages of a craft run under the Crafting Computer's threads and take no queue on the Mainframe
([Autocrafting](autocrafting.md)). A system that needs parts of its own (the AI, the simulator, security, robotics)
brings its own SubOperation types.

### Bottlenecks

Every Operation with a queue runs at that queue's speed, Operations beyond the queues wait in PENDING, and Subframes
lend capacity at 0.6 and their cards' queues ([Computers](computers.md)). The bottlenecks, from the most to the least
common in practice:

1. Too few queues, or slow queues (weak graphics cards), for the number of Operations.
2. A memory buffer smaller than the processors' capacity (processors idle, waiting for memory).
3. Slow disks (an HDD waits 10 ticks).
4. A weak processor or weak memory on the source or destination server.
5. A network cable with too little throughput (the slowest on the path).

What the Mainframe's log shows for the SELECT above:

```
[14:32:10.001] OPERATION START: SELECT (id=abc-123) src=PC-Steve FOR cobblestone×1000 → PC-Steve
[14:32:10.001] LOCK: cobblestone×600 on Server_A. cobblestone×400 on Server_B.
[14:32:10.001] DISPATCH SUB_SELECT abc-123-1 → Server_A (600 items)
[14:32:10.001] DISPATCH SUB_SELECT abc-123-2 → Server_B (400 items)
[14:32:12.340] PROGRESS abc-123-1: 420/600 (70%), Server_A
[14:32:13.100] PROGRESS abc-123-2: 280/400 (70%), Server_B
[14:32:14.210] SUB_COMPLETED abc-123-1, Server_A, 600 items in 4,209 ticks
[14:32:14.890] SUB_COMPLETED abc-123-2, Server_B, 400 items in 4,889 ticks
[14:32:14.891] OPERATION COMPLETED: SELECT abc-123, 4,890 ticks, 1000 items
[14:32:14.891] UNLOCK: cobblestone×1000
```

## The log and the statistics

- The Mainframe saves the **log** of the last 32 Operations and the persistent Operations. When it starts, it rebuilds
  them once booting has settled (the index analysed, the Crafting Computers recognised), with the time an orphaned
  Operation is kept (24 hours by default). A multi-stage finds its stages by id; the machine stages inside a recursive
  craft are not saved. The log prints on the printer ([Peripherals](peripherals.md)).
- The **IQL Server Profiler** records a trace (instructions, Operations, locks, plans, what the buses moved) and keeps
  it in a file on the computer.
- **Statistics**, per Operation type, for the last hour: Operations per hour, average wait, average run and failures
  (the Network Manager's Stats tab, the `stats` command, `Mainframe.Stats` in Σ#; [Interface](interface.md)).

## Network engines

The Mainframe doesn't plan on its own: it runs an **engine**, a program installed on it. All demand for work comes in
through the network's Operations service and goes to the running engine: computers connect to the **network**, not to
the engine.

| Engine | From | What it adds |
| --- | --- | --- |
| Midsoft IQL Server | every Mainframe | The default on every new Mainframe: IQL with views and procedures. Versions by era: 4.2 (Vintage), 2000 (Legacy), 2008 (Transition), 2012 (Standard), 2022 (Advanced). |
| NextgreIQL | Legacy | An explicit plan by cost: EXPLAIN and EXPLAIN ANALYZE; hints after a statement (PREFER SOURCE, AVOID SOURCE, MAX PARALLEL, PREFER MACHINE, PREFER BENCH); rules, statistics and other mods' notes in the planner. Versions 7.0, 8.3, 9.0 and 16. |
| Prophet YourIQL | Legacy | Declarative: the player declares a state (KEEP item >= n, BETWEEN; WATCH ... DO; CRAFT item TO n; FORGET; SHOW STATES) and it finds the Operations that reach and keep it. Versions 3.23, 5.0, 5.6 and 8.0. |

- **Without an engine**, pulling, pushing, moving, exporting and filling still work, but crafts, plans and IQL are
  refused ("Network Operations Service unavailable").
- The engine is the Mainframe's choice. A Subframe has to run the **same** engine to lend capacity and queues
  ([Computers](computers.md)).
- **Changing or stopping** the engine: only new work goes to the new one; the Operations that already exist finish on
  the plan they had, even after a reload. The change takes from 2 seconds to 2 minutes, by the size of the catalogue.
- **Jobs** (EVERY, WHEN) belong only to the Automation Engine, the same for every engine.
- The engine's memory weighs on the Mainframe ([Hardware](hardware.md)).
- Add-ons register engines ([The API](api.md)).

Each house has its tools: the IQL Server Management Studio (ISMS) and the IQL Server Profiler (Midsoft), the Nextgre
Planner Studio, the Prophet Reactive Console, and the Automation Manager ([Programs](programs.md)). A program written
for one engine says so when it opens on a network running another.

## IQL, the Item Query Language

IQL is the network's language: a dialect of SQL made for the mod. It is used:

- at the prompt of any system (`iql`, which also answers to `operation`, `op` and `sql`, and `interac`, the Interactor's
  command-line client);
- in `.iql` files: one statement per line, `--` comments, `;` between statements, and parameters asked for when it runs:
  `CRAFT <count, number, 64> <item, name, torch>`;
- in Σ# programs (`Iql.Run`, `Query`, `Exec`, `RunFile`, [Σ and Σ#](sigma.md));
- to set up devices by name: `SET BUS 'name' ...`, `SET REDSTONE 'name' ...`, `SET INTERFACE ...`, `SET ROUTER ...`.

What is written:

- **Requests:** SELECT, INSERT, DELETE, MOVE, DROP, CRAFT, COUNT, LOCK and UNLOCK, each with an amount and an item, and
  optionally FROM, TO, WHERE, IF, ORDER BY, LIMIT and PRIORITY (LOW, MEDIUM_LOW, MEDIUM, MEDIUM_HIGH, HIGH).
- **Questions:** QUERY or SHOW and what to look at (items, disks...), with WHERE, ORDER BY and LIMIT.
- **Upkeep:** ANALYZE, VACUUM, REINDEX.
- **Changing items:** `UPDATE ... SET SMELT`, `ENCHANT [OFFER n]`, `REPAIR`, `COMBINE ... WITH ...`, `NAME '...'`.
- **Saved work** (on the engines that offer it): CREATE VIEW, CREATE PROCEDURE and EXEC; CREATE JOB with EVERY a time or
  WHEN a condition, for the network to run on its own.

```
SELECT 64 iron_ingot
CRAFT 64 torch
QUERY items WHERE qty > 100
QUERY items ORDER BY name LIMIT 2
QUERY disks
```

Every engine and every version has its own dialect. A line that is refused says where reading stopped.

## To build

- **DROP without asking.** A DROP runs with no confirmation, no phrase to type and no question, as on a real database.
  Today the ISMS asks before a DROP.
- **UPDATE costs energy**, taken from the computer that has the cards: smelting, enchanting, repairing, combining and
  naming each spend their own FE (estimates), and the cards draw their watts like any card ([Power](power.md)). The
  server's settings (`jscomputers-server.toml`, `[update] cost`) choose what is paid: FE (the default), XP (levels of
  the one asking, fewer than the vanilla blocks and with no lapis lazuli), both, or nothing. The Workshop pays the same
  ([Programs](programs.md)). Today enchanting and the anvil cost levels, and the furnace nothing.
- **The network's event log**, apart from the Operations log, records what happens to every machine, with **levels**:
  INFO (turned on, turned off), WARN (a degraded power supply, a UPS on battery, stock below a threshold) and ERROR (a
  network conflict, a power loss, items lost, corruption, a failed power supply, a WAN link going down). It is read in
  the Network Manager's Log tab ([Interface](interface.md)), with filters by level, machine and period; it prints, and
  it **exports** to a file on a disk. The portable devices' notifications come from it ([Wireless](wireless.md)). The
  **audit** log (who did what) belongs to security ([Security](security.md)).
- **Access control:** GRANT, REVOKE and AUTH are security's Operations. They are checked when the dispatch admits an
  Operation, in O(1); a network without a directory allows everything; permissions belong to players, and ID Cards serve
  physical access ([Security](security.md)).
- **Longer statistics:** see [Interface](interface.md).
