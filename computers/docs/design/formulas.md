# Formulas

Every formula of the mod in one place, with the page where each one lives. All the numbers are estimates; the ones that
can change without touching the code are in the settings ([Implementation](implementation.md)). No derived number is
kept on the parts: it always comes from the formula, and a computer keeps the result until a part goes in or comes out.

## Worked out when the hardware changes

| What | Formula | Page |
| --- | --- | --- |
| A processor's capacity | 40 × (P cores × GHz × efficiency × 1.2 with SMT + E cores × E GHz × E efficiency), rounded, never below 1 | [Hardware](hardware.md) |
| A computer's capacity | the sum of its processors | [Hardware](hardware.md) |
| The network's orchestration capacity | Mainframe + Σ (Subframe × 0.6) | [Computers](computers.md) |
| The memory buffer | GB × 256 items; the Mainframe works at the smaller of its capacity and its buffer | [Hardware](hardware.md) |
| A graphics card's power | units × GHz × efficiency, rounded, never below 1 | [Hardware](hardware.md) |
| A graphics card's threads | cores × 4 | [Hardware](hardware.md) |
| The slot cut | 1 at the card's generation or newer; 1/2 one generation behind, 1/4 two, never below 1/8; it applies to power, video memory and a Cluster Interface Card's installs | [Hardware](hardware.md) |
| A graphics card's queue on the Mainframe | max(1, min(processors' capacity, power × slot cut)) | [Hardware](hardware.md) |
| The Mainframe's queues | 1 + the Mainframe's graphics cards (+ the ones Subframes lend); they don't share capacity | [Operations](operations.md) |
| A server's transmission bonus | threads × 0.05 items per tick | [Hardware](hardware.md) |
| A rack's thermal budget | 1,000 W + 1,500 W × Cooling Units; over it, every machine runs at max(0.25, budget / load) | [Temperature](temperature.md) |
| A Crafting Computer's crafting speed | processor capacity × the sum of its cards' factors | [Autocrafting](autocrafting.md) |
| The crafts of a coprocessor slot | 8 × 2^(N-1); 504 with all six slots | [Autocrafting](autocrafting.md) |
| A machine's instruction credits per tick | max(32, Σ (cores × MHz) / 8) | [Σ and Σ#](sigma.md) |
| An item's size | on a disk, by the disk's era: 1 MB (Vintage), 16 MB (Legacy), 256 MB (Transition on); in memory, 4 MB | [Storage devices](storage-devices.md) |
| Starting with a power supply | it starts if the parts' watts ≤ the supply's rating | [Power](power.md) |

## Per Operation and per tick

| What | Formula | Page |
| --- | --- | --- |
| A disk's wait | HDD 10, SSD 3, NVMe 1 ticks (configurable); −25% with a Cache Card in the bay, −15% with the Predictive Cache on the server | [Storage devices](storage-devices.md), [Storage](storage.md) |
| An External Storage Bus | 10 × a hard disk's wait | [Storage](storage.md) |
| An Import Bus | flushes when its batch fills (the "max", or the speed × 20), every 20 ticks, or when the item type changes | [Storage](storage.md) |
| A waiting Operation's aging | up one priority level every 600 ticks (configurable; 0 never) | [Operations](operations.md) |
| Giving up on an Operation in WAITING | 1,200 ticks (configurable) | [Operations](operations.md) |
| An Operation's throughput budget | the smallest of its queue's speed, the memory buffer, the slowest cable to the server, the server's processor and memory capacity, and the disk | [Operations](operations.md) |
| The cost of a Σ# program's call | 5 (the machine about itself), 10 (the network), 30 (gathering a list), 50 (reading), 100 (writing), 200 (asking the network), +1 per row returned | [Σ and Σ#](sigma.md) |
| The programs' clock | 1,000 µs per machine per tick, 8,000 µs for the whole server (configurable) | [Σ and Σ#](sigma.md) |

There is no "effective capacity" formula (the total minus what programs take): programs take no capacity
([Programs](programs.md)).

## To build

The formulas of the systems still to build:

| What | Formula | Page |
| --- | --- | --- |
| Energy drawn | the parts' watts / the supply's efficiency, in J's Core's real units (a setting turns the draw off) | [Power](power.md) |
| A power supply's wear | the line's base life (60, 100, 150, 250 game days at full load) × 8 up to 50% load, × 3 from 50 to 80%, × 1 above; half in a rack that is throttling; ±20% drawn once; degraded at 80%, failed at 100% | [Power](power.md) |
| Temperature | the surroundings + the rise from the load; load = thermal budget gives 80 °C; from 80 to 90 °C down to −15%; at 90 °C it cuts down to a floor of 25%; at 100 °C, thermal trip | [Temperature](temperature.md) |
| A UPS's battery | runtime = battery in FE / draw; clean shutdown at 10% | [Power](power.md) |
| A WAN Gateway Computer's speed | min(the line's speed, the gateway's capacity) | [The network](network.md) |
| A Teracoin block | each miner's chance per block = hashrate / difficulty; hashrate from processor capacity and graphics card power; adjusted every 10 blocks, with a floor from the world's days; the reward halves every N blocks | [Teracoin](teracoin.md) |
| An item's dataset | 64, 256, 1,024, 4,096 samples give 50, 75, 90, 99% highest accuracy | [Artificial intelligence](ai.md) |
| Training | work = samples × tokens per item × 3; time = work / tokens per tick of the graphics and compute cards | [Artificial intelligence](ai.md) |
| Generating | items per tick = tokens per tick × the architecture's factor / tokens per item; only items whose tokens fit in the context window; hits per batch by accuracy; the part of a model offloaded to memory at about 1/10 | [Artificial intelligence](ai.md) |

The formulas of hacking come with security's second phase ([Security](security.md)).
