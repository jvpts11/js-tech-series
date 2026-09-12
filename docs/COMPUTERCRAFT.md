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

Our shared folders are mounted on their computers as `/jsc/<host>/<share>`, readable or writable
according to what the sharing computer and the Gateway both allow.

## What one of our programs can ask of them

From Cannon, through `Gateway`: which computers and peripherals are on the other side, calling any of
their peripherals by name, turning their computers on and off, and sending them a line. With the agent
running, also: running a program or a line there, and reading, writing or listing their files. The
language reference has the shape of every call, on the [Cannon page](CANNON.md).

## The agent

Their computers carry a small program of ours, which starts with the computer. On a computer with no
Gateway within reach it does nothing and is over in a line. Where there is one, it says it is there and
answers what our side asks of it.

It is not written in their language by hand: it is one of our own programs, translated on the way out by
the same translator every program of ours goes through, and given to their computers as a data pack of
one file. If our translator were ever wrong, the agent would be wrong with it and say so at once, which
is the point of building it that way.

The agent is also why `Gateway.Run`, `Shell`, `Read`, `Write` and `List` need the computer to be ON: a
computer that is off has no agent, and those calls say so rather than pretending.

## Programs crossing

```
jsc run <program> [words]    at their prompt: one of the host computer's programs, run there
jsc list                     what there is to run
```

```
gateway cc-bridge get 3:/reactor.lua C:\cc\reactor.lua      at ours: a file from their computer
gateway cc-bridge put C:\prog\batch.can 3:/batch            a file to it
```

A program of ours is compiled and translated as it goes; one of theirs crosses untouched. There is no
flag for this and no second copy of anything: translation is what going to another kind of computer
means. The file explorer shows their computers under Network, as ComputerCraft, and their folders are
browsable there.

A translated program is the same program, but their computers count in one kind of number, so a whole
`3.0` prints as `3` over there, and very large whole numbers lose their last digits crossing.

## What it costs, and what it is allowed

Every call across the bridge is paid for by the Gateway's host computer, out of the same budget its own
programs run on: a ComputerCraft computer hammering the bridge slows that computer, not the server. The
Gateway answers a fixed number of calls a tick (4, 8 or 16) and refuses the rest with `busy`.

Three switches, in the **Gateway Manager** program or with `gateway <name> set`:

- **read** the network: totals, types, servers, watches.
- **operations**: pulling, pushing, crafting, and running things. Allowing it is authority over the
  computer on the other side, its own files and peripherals included, because running a line there runs
  code with that computer's powers.
- **files**: off, read, or read and write, for the shared folders and for reaching into their disks.

There is also a ceiling on the priority a request from their side may carry, so their computers cannot
outrank the network's own work.

Everything refused is written into the Gateway's log, which the Gateway Manager shows and
`gateway <name> log` prints.

## Limits worth knowing

- An answer from one of their computers is measured before any of it reaches a program of ours: 64 KB of
  text, 4096 things in a list, eight deep, 256 KB in all. Bigger than that is refused whole.
- Sixty-four questions may be in flight at once per Gateway.
- A question that is still out when the world is saved is not asked again when it is read back.
- `rednet` and `gps` are not bridged yet.
