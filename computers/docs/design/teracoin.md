# Teracoin

Teracoin (TRC) is the cryptocurrency of the world. Nothing on this page exists yet.

## To build

### The coin

Teracoin is one chain of blocks per world, owned by nobody, with one difficulty and one halving for the whole server,
never per network (making new networks escapes nothing). Single player has its own the same way. The chain is saved as
the world's state.

A **wallet** is a file on a disk, made by the Crypto Wallet, that keeps the key of an **address** (`trc1q8f3...`). The
address belongs to the wallet, not to the machine: taking the file to another computer takes the address, and two
wallets on the same machine are two addresses. TRC live on the chain, at the address. Copying the file copies the
access, not the coins (whoever has a copy spends the same balance); losing every copy loses the TRC for good.

### Mining

Mining is software: a **miner program** runs on a computer and spends its hashrate, which comes from the formulas that
already exist, the processors' capacity and the graphics cards' power ([Hardware](hardware.md)). It follows real
history: processors mine at the end of the Transition, graphics cards rule in the Standard, and ASICs arrive from the
Standard on.

- **The Mining Computer** is the mining **rig**, the cheap option. A computer with no case, an open frame with mounting
  points that hold everything: motherboard, processor, memory, power supply and several graphics cards on risers, all in
  view on the model. It is a real computer: it runs a system and the miner. It exists per era, from the Transition on.
- **An ASIC** is a device made only to mine, per era (Standard and Advanced). Much more expensive in resources and in
  energy, but it earns much more.
- **The payout address:** every miner (the program on a rig, or an ASIC) has a payout address, set in its window or at
  the prompt (`miner --address trc1...`); the Crypto Wallet shows a wallet's address on its Receive button. Two players
  on the same network each put their own wallet's address on their machines.
- **Energy** is the real cost: mining draws the parts' watts ([Power](power.md)).

### Pools, the panel and the wallet

- **The Mining Pool:** a service on a rack server ([Servers and racks](servers-and-racks.md)). It gathers the network's
  miners that choose it and pays each address its share of the work: a steady income instead of a block now and then.
- **The Mining Panel:** a desktop program that runs on any computer on the network and connects to its pools (the same
  pattern as the Messenger and the Messenger Service): the hashrate, TRC per hour, day and week, the blocks found, each
  miner, each address's share, the difficulty and the projection. Without a pool, each miner shows its own figures in
  the miner program, like a real miner's console.
- **The Crypto Wallet:** a program, from the Transition on. It makes wallets, shows the balance and the history, sends
  to an address and shows its own (Receive).

Managing a fleet of rigs is this software, not a computer of its own ([Computers](computers.md)).

### Blocks

- A block comes out every **target time** (an estimate: 10 game minutes). Each miner's chance is its hashrate divided by
  the difficulty.
- The difficulty adjusts every 10 blocks to keep the target time, with a **floor** that grows with the world's days:
  turning everything off for days and coming back doesn't give easy blocks.
- The **reward** is a fixed amount of TRC, which halves every N blocks (the **halving**), for the whole server. Now and
  then, a block also brings a rare item, from a list in the settings.
- It is worked out once a second per miner that is on; a miner that is off costs nothing.

### The 3D Printer

A peripheral per era (Standard and Advanced), linked by the peripheral cable, that turns TRC into items. The **prices**
are in a section of the server's settings: every line is an item and its price in TRC, and the list **is** the list of
what can be printed ([Overview](overview.md)). The mod brings a starting list with the valuable vanilla items. Printing
automatically from a balance is a setting of the Crypto Wallet. The 3D Printer doesn't replace crafting: it turns
digital wealth into rare items.

### Settings

In `jscomputers-server.toml`, `[teracoin]`: the block's target time, the reward, the halving (on or off) and its
interval in blocks, the difficulty floor's factor, the list of rare items from blocks and, in `[teracoin.printer]`, the
3D Printer's prices. An operator restarts the halving with `/jstech crypto halving reset`, which puts the reward back to
its first value. The numbers are estimates.

J's Economy is meant to use Teracoin as its currency when both mods are installed; how is designed with J's Economy
([The series](series.md)).
