# Storage

Where the network keeps things: each computer's local storage, the network's storage, the buses that join it to the
world, chemicals, and what happens when storage gets corrupted. The disks themselves are in
[Storage devices](storage-devices.md).

## Local storage

Every computer has local storage: the disks installed in it and its **storage slots** (the computer's own shelf of
items, [Computers](computers.md)).

Items requested with a SELECT arrive in the local storage of the computer that asked, not in the player's own inventory;
the player takes them from there. The same path serves an INSERT by hand: handing things over follows a single rule
(below). The network doesn't care about distance, only about the path and its cables.

- **Servers:** their storage is always public. There is no privacy slider. Every disk of a server (the ones in its rack
  bays) shows in the network's index and can be reached by Operations.
- **Personal Computer:** a slider per disk sets what part is public and what part private (in thousandths). A new disk
  starts all private. The private part doesn't show in the index and can't be reached by other players' Operations or by
  automation. The public part joins the index **read only**: the network reads from it, as the last resort of a SELECT,
  but never writes to it.
- **The other computers** (Mainframe, Crafting Computer, Cluster Management Computer) have no slider: their disks hold
  their system, their network engine and their programs, and are not part of the network's storage.

The network's index lives in the Mainframe's memory (not on disks): it is volatile, rebuilt every time the Mainframe
starts ([Operations](operations.md)).

## The network's storage

The network's storage is the servers' disks (in the racks' bays), the public part of Personal Computers (read only) and
the inventories the External Storage Buses face. Each is a place with its own **effective wait**: its disk's
([Storage devices](storage-devices.md)), cut by the bay's Cache Card and by the server's Predictive Cache
([Servers and racks](servers-and-racks.md)), and its server's memory wait ([Hardware](hardware.md)).

### Where the network reads and writes first

- **Reading** (SELECT and the like): the Mainframe serves first from the place with the **shortest effective wait** and,
  on a tie, from the one with the **most** of that item. The public part of a Personal Computer only comes in as the
  last resort.
- **Writing** (INSERT and the like): by **storage priority** (the network's is 0; an External Storage Bus from the
  Standard on can be set above or below it), then to the fastest and emptiest. Inside a datacenter section, the
  section's balancing mode decides ([Servers and racks](servers-and-racks.md)). The network never writes to the public
  part of a Personal Computer.

## Buses

Buses are **ports** of the network: thin parts mounted on the faces of a data cable (Access or Backbone; never High
compute, long distance or peripheral), one per face, facing the block next to it. What passes through them is the
network's Operations, logged. Every bus is a **named** device of the network (a unique name), reached by IQL
(`SET BUS 'name' ...`), by Σ# (`bus("name")`) and by the Automation Manager.

Its window, in its era's skin, has three tabs: what is set up (Configure), what it did and what it held and why
(Activity), and the IQL and Σ# lines that set it up the same way (Software). A value set by a program carries the
program's mark. A bus is turned on and off, and works all the time or only with a redstone signal.

- **Import Bus:** brings items from the inventory it faces into the network (INSERT), with an internal buffer it flushes
  in batches: when the batch fills, every 20 ticks, or when the item type changes. The batch size is the "max" set up
  (or the speed × 20). This cuts the index's updates a great deal on busy sources: a quarry at 3 items per tick gives
  about one flush every 21 ticks instead of one INSERT per item. If the network is full, the bus holds on and keeps
  gathering.
- **Export Bus:** sends items from the network to the inventory it faces (DELETE), with a filter, an amount to keep at
  the destination and a maximum.

When several buses compete for the network, priority decides, taking turns.

What each era can do (never faster than its cable carries):

| Era | Speed | What can be set up |
| --- | --- | --- |
| Vintage | 1 item per tick | nothing: one type at a time, no filter |
| Legacy | 8 | a filter (5 items, only-these or all-but), keep and max |
| Transition | 16 | a filter, and keep and max **per item** on the list |
| Standard | 32 | a filter, keep and max, **priority** and **conditions** (stock under N of an item or a #tag, time of day, after another bus finishes) |
| Advanced | 64 | the Standard's, plus filtering by **tag** and loose (fuzzy) matching |

**External Storage Bus:** it moves nothing on its own: the network uses the inventory it faces (a vanilla chest, other
mods' machine inventories) as storage of its own, which shows in the index as if it were a server. It is **ten times**
slower than a server (10 times a hard disk's wait). The privacy slider doesn't apply: everything that goes through an
External Storage Bus is public. An item counts as 4 MB inside it. By era: the Vintage one shows everything; the Legacy
and Transition ones show what their filter lets through and choose the access (read and write, read only, write only);
the Standard one adds storage priority; the Advanced one, tags and loose matching.

There is no Message Bus and no Redstone Bus face: the Redstone Interface is a peripheral
([Peripherals](peripherals.md)), and messages come from programs.

## Chemicals, the third kind of data

A storage key has three kinds: ITEM, FLUID and CHEMICAL. A chemical (the gases and substances of mods like Mekanism) is
known by its registry id and weighs like a fluid, in mB (1,000 mB = one item). The bridge to the mod that defines them
(in J's Core) only exists when that mod is present; without it, nothing in the mod mentions chemicals, not even the
tooltips. Buses, disks, terminal and Operations treat the three kinds the same way. The terminal and the desktop draw a
fluid with its texture and a chemical with a colour swatch, with the amount in mB.

## Handing things over by hand

Putting things in and taking them out by hand follows **one rule** on **every** route (terminal and desktop, the network
tab and the local tab):

- a left click or a shift-click with a stack on the cursor stores the **item** as it is; a full bucket goes in as a
  bucket;
- a right click on the grid with a full container on the cursor hands over what it holds (one container per click) and
  gives it back empty to the cursor;
- a right click on a fluid or chemical input with an empty container fills it from there. A bucket only fills with a
  whole 1,000 mB; an item tank takes what fits.

Any new route for handing a stack to a computer follows this rule.

## To build

### Corruption

Corruption never happens in normal play. It only comes from exceptional events: a **network conflict**
([The network](network.md)), a **thermal trip** (a chance of corrupting the disk of the machine that shut down,
[Temperature](temperature.md)) and, with security, **viruses** and **hacking** ([Security](security.md)).

- A share of the stored items (10% in a conflict, an estimate) becomes **corrupted**, marked inside the volume.
  Corrupted items leave the index and can't be requested; the Storage Insights and This PC show how much of each disk is
  corrupted. They don't vanish at once: they wait for the Defrag.
- It can also hit a disk's **system files**; a system missing files may not start
  ([Operating systems](operating-systems.md)).
- Heavy corruption (over 50%) damages the disk itself: **bad sectors**, a field of the volume that reduces its capacity;
  over 90% the disk dies.
- The private part of a Personal Computer is never affected.

**The Defrag** is the classic defragmenter, a program every computer comes with. It runs on the computer and tries to
recover each corrupted item, with a chance by disk kind (estimates):

- a hard disk recovers almost always;
- an SSD recovers while the damage is under 70% (a base chance of 70%, minus 10 points for every 10% more damage);
- an NVMe disk while it is under 50% (a base chance of 60%, minus 15 points for every 10%).

What isn't recovered is lost. The Defrag also tries to repair part of the bad sectors. With luck, almost everything
comes back. A system that won't start is fixed by taking the disk to a Dock Station on another computer and running the
Defrag there, or by installing it again. The mechanics and the numbers will be tuned when it is built.

**Settings:** in `[item_loss]` ([Power](power.md)), the `corruption` key turns all corruption off, and
`network_conflict` only the corruption that comes from a conflict.

### Every state of matter

J's Core splits matter into its states: items, liquids, gases, plasmas, supercritical fluids, slurries and exotic
matter. The storage key follows: its kinds become items, liquids, gases and plasmas (and the rest as J's Core declares
them), with other mods' chemicals still coming in through the bridge. Buses, disks, the terminal and Operations treat
every kind the same way, as they do the three of today.
