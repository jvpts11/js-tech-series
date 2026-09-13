# ComputerCraft

J's Computers and CC: Tweaked are two families of computers in one world, and they can be made to work
together. This page is how: what the bridge between them is, what each side can ask of the other, and
what it costs.

CC: Tweaked is optional. Without it, everything here simply is not there, and nothing else about the mod
changes.

## The Network Gateway

The bridge is a block: a **Network Gateway**. It is a device, not a computer. It has no processor, no
memory and no system of its own, and it does nothing until it is linked to one of our computers through
a peripheral cable on its back socket. From then on it borrows that computer: its network, its name, and
its budget.

Its front face is a ComputerCraft peripheral of type `jsc_gateway`, and it joins their wired networks
like any other peripheral. Its sides are vents, and its top and bottom open onto a buffer of nine slots
where items wait on their way across.

A computer can have more than one Gateway. Each has a name (`gateway-1` by default), and everything
below can be addressed to one of them by that name.

## What a ComputerCraft program can ask of us

Wrap the peripheral and ask:

```lua
local jsc = peripheral.find("jsc_gateway")

jsc.online()                         -- whether the Gateway's computer is on our network
jsc.name()                           -- which Gateway this is
jsc.host()                           -- the computer it is linked to

jsc.total("minecraft:iron_ingot")    -- how much the network holds
jsc.types()                          -- everything it holds, by name
jsc.find("minecraft:iron_ingot")     -- which servers hold it
jsc.servers()                        -- the network's servers and their use
jsc.capacity() / jsc.used()
jsc.computers()                      -- our computers, and what they share

jsc.pull("minecraft:iron_ingot", 64) -- into the Gateway's buffer; the operation's id
jsc.push("minecraft:iron_ingot", 64) -- out of the buffer and into the network
jsc.craft("minecraft:hopper", 8)
jsc.operation(id) / jsc.operations() / jsc.cancel(id)

jsc.run("desk", "reactor.asm")       -- start one of our programs on one of our computers
jsc.watch("minecraft:iron_ingot")    -- be told when the total moves
jsc.log("info", "hello")             -- write a line into the Gateway's log
```

Names are what CC programs expect: an item is its registry id (`minecraft:iron_ingot`, and the
`minecraft:` may be left off), a fluid is `fluid/` and its id, a chemical `chemical/` and its id.

Two things arrive as events on the computer that asked for them: `jsc_stock(name, total, previous)` when
a watched total moves, and `jsc_operation(id, status)` when an operation of theirs settles.

## What one of our programs can ask of them

From Cannon, through `Gateway`: which computers and peripherals are on the other side, calling any of
their peripherals by name, turning their computers on and off, and sending them a line. The language
reference has the shape of every call, on the [Cannon page](CANNON.md).

The bridge stops there on purpose. A program of ours does not reach into one of their computers' files
or prompt, and a program does not cross from one kind of computer to the other: each side is programmed
in its own language, and the Gateway is what they share. Their computers speak Lua, ours speak Cannon,
and the network is the thing in the middle.

## What it costs, and what it is allowed

Every call across the bridge is paid for by the Gateway's host computer, out of the same budget its own
programs run on: a ComputerCraft computer hammering the bridge slows that computer, not the server. The
Gateway answers a fixed number of calls a tick (4, 8 or 16) and refuses the rest with `busy`.

Two switches, in the **Gateway Manager** program or with `gateway <name> set`:

- **read** the network: totals, types, servers, watches.
- **operations**: pulling, pushing, crafting, and starting one of the network's own programs.

There is also a ceiling on the priority a request from their side may carry, so their computers cannot
outrank the network's own work.

Everything refused is written into the Gateway's log, which the Gateway Manager shows and
`gateway <name> log` prints.

## Limits worth knowing

- `rednet` and `gps` are not bridged yet.
