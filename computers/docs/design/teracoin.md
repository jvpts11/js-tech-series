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
access, not the coins (whoever has a copy spends the same balance); losing every copy loses the TRC for good. A TRC
divides into eight decimal places, as a bitcoin does.

### Mining

Mining is software: a **miner program** runs on a computer and spends its hashrate, which comes from the formulas that
already exist, the processors' capacity and the graphics cards' power ([Hardware](hardware.md)). It follows real
history: processors mine at the end of the Transition, graphics cards rule in the Standard, and ASICs arrive from the
Standard on.

- **The hashrate**, in hashes per second: a processor mines at its capacity, a graphics card at its power × 10. On a
  rig's risers mining ignores the slot cut, as real mining does. A Transition processor gives about 230 H/s, a
  Transition graphics card about 770, a Standard one about 40,000 and an Advanced one about 200,000: processors lead at
  the end of the Transition, graphics cards in the Standard, and the ASICs pass them.
- **The Mining Computer** is the mining **rig**, the cheap option. A computer with no case, an open frame with mounting
  points that hold everything: a motherboard, a processor, memory, a power supply, one disk and graphics cards on risers
  (4 in the Transition, 6 in the Standard, 8 in the Advanced), all in view on the model. It is a real computer: it runs
  a system and the miner, with no monitor needed, reached by ssh or the Mining Panel. It exists per era, from the
  Transition on.
- **An ASIC** is a block made only to mine, per era: the Standard's mines 1,000,000 H/s at 1,400 W, the Advanced's
  10,000,000 at 3,200 W. It has no system: its window sets its payout address. Much more expensive in resources and in
  energy, but it earns much more.
- **The payout address:** every miner (the program on a rig, or an ASIC) has a payout address, set in its window or at
  the prompt (`miner --address trc1...`); the Crypto Wallet shows a wallet's address on its Receive button. Two players
  on the same network each put their own wallet's address on their machines.
- **Energy** is the real cost: mining draws the parts' watts ([Power](power.md)).

### Pools, the panel and the wallet

- **The Mining Pool:** a service on a rack server, from the Transition on ([Servers and racks](servers-and-racks.md)).
  It gathers the network's miners that choose it and pays each address its share of the work: a steady income instead
  of a block now and then. The pool mines with its miners' hashrate together; when it finds a block, the reward is
  shared in proportion to the work each miner gave (its hashrate × the time it mined) since the pool's last block,
  less the pool's fee, set by its owner from 0 to 5% (1% by default) and paid to the owner's address.
- **The Mining Panel:** a desktop program that runs on any computer on the network and connects to its pools (the same
  pattern as the Messenger and the Messenger Service): the hashrate, TRC per hour, day and week, the blocks found, each
  miner, each address's share, the difficulty and the projection. TRC per hour is the hashrate ÷ the difficulty × 3,600
  × the reward. Without a pool, each miner shows its own figures in the miner program, like a real miner's console.
- **The Crypto Wallet:** a program, from the Transition on. It makes wallets, shows the balance and the history, sends
  to an address and shows its own (Receive). A send has no fee: it is pending until the next block confirms it, or for
  10 minutes when no block comes, so the coin never stalls on a server where nobody mines.

Managing a fleet of rigs is this software, not a computer of its own ([Computers](computers.md)).

### Blocks

- A block comes out every **target time** (an estimate: 10 minutes, 12,000 ticks). Each second, each miner's chance of
  finding it is its hashrate divided by the difficulty.
- The difficulty adjusts every 10 blocks to keep the target time: it is multiplied by the time those blocks should have
  taken over the time they took, never by more than 4 or less than a quarter at once, as the real one is. It starts at
  600,000 (about one Transition rig with a few cards) and has a **floor** that grows with the world's days, the start
  × (1 + days / 100): turning everything off for days and coming back doesn't give easy blocks.
- The **reward** is 50 TRC a block, halving every 2,100 blocks (about two weeks of a server running), for the whole
  server: 210,000 TRC ever exist, a nod to bitcoin's 21 million. One block in twenty also brings a rare item, from a
  list in the settings (by default diamonds, emeralds, netherite scrap, ancient debris and an enchanted golden apple).
- It is worked out once a second per miner that is on; a miner that is off costs nothing.

### The 3D Printer

A peripheral per era (Standard and Advanced), linked by the peripheral cable, that turns TRC into items. The **prices**
are in a section of the server's settings: every line is an item and its price in TRC, and the list **is** the list of
what can be printed ([Overview](overview.md)). Printing automatically from a balance is a setting of the Crypto Wallet.
The 3D Printer doesn't replace crafting: it turns digital wealth into rare items. An item takes 30 seconds to print,
half that on the Advanced printer. The mod's starting list, in TRC:

| Item | TRC | Item | TRC |
| --- | ---: | --- | ---: |
| Emerald | 5 | Totem of Undying | 60 |
| Diamond | 10 | Heart of the Sea | 100 |
| Music disc | 15 | Enchanted golden apple | 120 |
| Shulker shell | 20 | Trident | 150 |
| Ancient debris | 30 | Elytra | 400 |
| Netherite ingot | 80 | Nether star | 800 |

### Settings

In `jscomputers-server.toml`:

```toml
[teracoin]
target_minutes = 10                      # a block's target time
reward = 50                              # TRC a block, before any halving
halving = true
halving_blocks = 2100
floor_days = 100                         # the difficulty floor grows by its start every this many world days
rare_item_chance = 0.05                  # the share of blocks that bring a rare item
rare_items = ["minecraft:diamond", "minecraft:emerald", "minecraft:netherite_scrap", "minecraft:ancient_debris",
              "minecraft:enchanted_golden_apple"]

[teracoin.printer]
prices = ["minecraft:emerald=5", "minecraft:diamond=10", "..."]   # the list is what can be printed
```

An operator restarts the halving with `/jstech crypto halving reset`, which puts the reward back to its first value.
The numbers are estimates.

J's Economy is meant to use Teracoin as its currency when both mods are installed; how is designed with J's Economy
([The series](series.md)).
