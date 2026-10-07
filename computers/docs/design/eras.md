# Eras

An era is a generation of computing. There are seven of them, and the first five have parts:

| Era | Period | Word | An item on a disk | Instruction set | Firmware | Systems come on | Programs come on |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Vintage | the 80s and 90s | 16 bits | 1 MB | IA-16 | text BIOS | floppy | floppy |
| Legacy | 2000 to 2004 | 32 bits | 16 MB | x86 | blue BIOS | CD | CD |
| Transition | 2005 to 2010 | 64 bits | 256 MB | x86-64 | blue BIOS | DVD | DVD |
| Standard | 2011 to 2016 | 64 bits | 256 MB | x86-64 | UEFI | USB stick | DVD; services on USB |
| Advanced | 2017 to 2026 | 64 bits | 256 MB | x86-64 | UEFI | USB stick | Blu-ray; services on USB |
| Exa | from 2027 | | | | | | |
| Singularity | after the Exa | | | | | | |

A newer era is faster at everything: more cores at higher clocks, better designs, more memory, faster disks and cables.

## Eras are not tiers

Eras always go by their full names, the same ones the game shows: Vintage, Legacy, Transition, Standard, Advanced, Exa
and Singularity. Hardware is never written as T0, T1 and so on, because that is how J's Industrial writes its industrial
tiers, and the two have nothing to do with each other. In the code, `HardwareEra` (in J's Core, with a stable level from
0 to 6 that is what blocks and saves keep) and `IndustrialTier` (T0 to T9) are separate types that never turn into each
other: a player can have an Advanced computer next to a T2 industry, or the other way round.

Nothing in J's Computers is separated by industrial tier. Every computer, peripheral, cable and part exists per era, or
starts at an era.

## What a machine of each era has

| | Vintage | Legacy | Transition | Standard | Advanced |
| --- | --- | --- | --- | --- | --- |
| Expansion slots | ISA, PCI, AGP 2x | AGP 4x and 8x, PCIe 1.0 | PCIe 1.0 and 2.0 | PCIe 2.0 and 3.0 | PCIe 3.0, 4.0 and 5.0 |
| Memory | SIMM, EDO, SDRAM | SDRAM, DDR, DDR2 | DDR2, DDR3 | DDR3 | DDR4, DDR5 |
| Device ports on the board | 2 | 4 | 6 | 8 | 10 |
| Video outputs per graphics card | 1 | 2 | 2 | 4 | 4 |
| Sound | a sound card, or the speaker in the case | a sound card | on the board, or a sound card | on the board | on the board |
| Peripheral cable, and its reach | beige, 8 | black, 12 | white, 14 | braided, 16 | space grey, 20 |
| Ports on a hub | 2 | 4 | 4 | 7 | 7 |
| Crafting Card: interfaces it drives and bench recipes it holds | 2 | 4 | 5 | 6 | 8 |
| Patterns in a Crafting Interface | 3 | 6 | 8 | 9 | 12 |
| Video memory one monitor block holds | 64 KB (1 colour), 256 KB (16 colours) | 4 MB | 16 MB | 64 MB | 256 MB |
| Memory of a program of the era that says none of its own | 1 MB | 16 MB | 48 MB | 96 MB | 256 MB |
| Systems that start here | MC-DOS, MC-NET, UNIX System V | Frames 95, Frames XP, FreeBSD, Debian, Ubuntu, Fedora, Arch, Gentoo | Frames 7 | Frames 10 | Frames 11 |

## What fits with what

- Every computer exists in every era, with that era's name (Vintage Personal Computer, Legacy Mainframe), and its case
  only takes a motherboard of its own era (and of the form factors the case takes, [Computers](computers.md)).
- A motherboard only takes processors and memory of its era, of its socket, and of the memory generations it lists.
- A motherboard takes expansion cards of any era whose slot belongs to the family of its bus (ISA, PCI, AGP or PCI
  Express), except sound cards, which only go in a board of their own era. A card newer than its slot runs at the slot's
  bandwidth ([Hardware](hardware.md)).
- A program runs on the instruction set it was built for and on every newer one, never on an older one: x86-64 runs x86
  and IA-16 programs, and IA-16 runs only its own ([Σ and Σ#](sigma.md)).
- An operating system needs hardware of its first era or newer.
- A rack takes servers of its era or older.
- A port takes the cable of its era and of every era before it, never of a later one. Two eras of the same cable line
  that touch do not join: they meet at a router of the newer era. Peripheral cables follow the same rule
  ([Peripherals](peripherals.md), [The network](network.md)).
- Each removable medium goes in its own kind of drive. A drive also reads older optical discs: a DVD drive reads CDs, a
  Blu-ray drive reads DVDs and CDs.
- Power supplies of any era go in any computer: a power supply is just power, and a new one runs an old board fine.

Moving up an era means a new computer (the new era's case), a new motherboard, new processors and new memory. Expansion
cards come along if their bus is of the same family; disks and power supplies always come along to the next era.

## Exa and Singularity

The Exa starts in 2027: it is the era in which computing at the scale of exaflops becomes something a player can own.
The Singularity comes after it. Neither has parts yet, and their hardware is still to be designed. Wherever a rule is
given era by era, both use the Advanced values until they have their own.

## Telling the eras apart

Each era has a colour, the colour of its screens, which a part's tooltip uses on the era line so the era reads before
the word does: Vintage phosphor green (#33FF33), Legacy blue (#245EDC), Transition sky blue (#3FA9E0), Standard blue
(#0078D4), Advanced violet (#9B59FF), Exa cyan (#00E5FF), Singularity white (#F2F2F2).

The creative menu has a tab with what every era shares and one tab per era with parts, all with the same shelves: PCs,
crafting computers, cluster management computers, Mainframes, server racks, rack bays, peripherals, networking,
components and programs.

## Makers and product lines

A commercial brand gets a parody; the codename of a design or a chip stays the real one (Haswell, Conroe, Kepler GK110).

| Brand | Stands for | What it makes |
| --- | --- | --- |
| Integra | Intel | processors, Phi coprocessors, graphics on the die (HD) |
| Velocion | AMD | processors; graphics cards from the Radiance HD 6000 on |
| Atrion | ATI | graphics cards until 2010 |
| Envya | NVIDIA | graphics cards; Tessera compute cards |
| Tridex | 3dfx | the Voodoo graphics cards |
| Artisan | Creative | the 3D Blaster graphics card; the Tone Blaster sound cards |
| Stratix | Kingston / Corsair | memory |
| Vaultis | Seagate / WD / Samsung | disks |
| MF | ASUS / Gigabyte | motherboards and power supplies |
| Forge Logic | FPGA accelerators | Crafting Cards |

- **Integra processors:** Pentix (from the Pentix 75 to the Pentix 4, Pentix D and Pentix G), Celer (entry level),
  Centro 2 Duo, Quad and Extreme (Transition), Centro c3, c5, c7 and c9, Centro Ultra (Advanced), and Servo (servers and
  workstations, with the Bronzo, Plata, Oro and Platina ranges in the Advanced).
- **Velocion processors:** 5x86, K5 and K6 (Vintage), Duro (entry level), Sprint XP, Sprint 64 and Sprint II, Semper,
  Ascent (Phenom), Optera (servers), FX, Fuse (processor and graphics on one chip), Awayken (desktops, Advanced),
  Threadkiller (high-end desktops) and Epic (servers and Mainframes).
- **Envya graphics:** Prism (the first 3D cards) and Vertex; Tessera (compute, for servers).
- **Atrion graphics:** Wonder, Rave and Radiance; Radiance passes to Velocion from the HD 6000 on.
- **Stratix memory:** the Layer line, with RDIMM modules for servers.
- **Vaultis disks:** Trench (Vintage hard disks), Link (Legacy IDE, Transition SATA SSDs), Keep (hard disks), Swift
  (SSDs) and Bolt (NVMe).
- **MF boards and power supplies:** Baby-AT, AT, ATX, EATX, EEB and MTX boards; PowerBasic, PowerGold, PowerPlat and
  ServerPSU power supplies.

The software houses (Midsoft, Nouvell Networks, Autodeck and the rest) are in [Programs](programs.md).

## How hardware is made

Today every part comes from the creative menu, with no recipes. The series' rule is that J's Computers never depends on
J's Industrial, and that this design describes how computing works on its own; how hardware is made, with and without
J's Industrial, is decided together with J's Industrial's design.

## The era a player has reached

J's Core keeps, per player, the era they have reached, as a progression axis (`jscore:hardware_era`). Advancements can
wait on it, and operators set it with `/jstech progress`.

## To build

- **Motherboards with slots in groups**, like real ones: the video slot (AGP or PCI Express x16) and, next to it, the
  slots for other cards (ISA and PCI on old boards, PCI and PCI Express x1 later; PCI lasts until about the Standard).
  For example "1x AGP 8x + 5x PCI" on a Legacy board, or "1x PCIe 2.0 x16 + 2x PCIe x1 + 2x PCI" on a Transition one.
  Graphics cards go in the video slot, other cards in a slot of their bus family, and the slot bandwidth cut applies per
  group. Legacy sound cards are PCI, as the real ones were, and so is the Legacy Crafting Card. Today each board has a
  single kind of slot, and the Legacy sound cards sit on AGP 8x and PCIe 1.0.
- **Disks only in machines of their era or newer:** a new machine reads old disks, an old one refuses a disk newer than
  itself (in rack bays, by the server's era). Today any disk goes in any computer.
- **Reaching an era by building:** a player reaches an era the first time they assemble and power on a working computer
  of that era, at the same moment as the "Build a working ... computer" advancements. Assembly counts, owning the parts
  does not, and the axis only moves forward. This lets J's Industrial and the other mods gate content on the era a
  player has reached. Today only an operator moves the axis.
