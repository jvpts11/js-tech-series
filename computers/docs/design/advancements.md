# Advancements

J's Computers has 69 advancements in four tabs, each with its own root:

| Tab | Count | What they are about |
| --- | ---: | --- |
| Hardware | 14 | assembling machines of each era, racks, the supercomputer (TOP500), memory filled to the top |
| Networks | 15 | the network, SELECT, automatic crafting, conflicts and deadlocks |
| Σ | 13 | writing, compiling, publishing and running programs |
| Systems | 27 | installing and using the systems (I Use Arch BTW, Format C:, RTFM, Press DEL to Enter Setup) |

Their names are jokes from computing culture, in English. Some are challenges.

They fire through the mod's own triggers, on the server, when the player does the thing (a system booting for the first
time, an Operation finishing, a program travelling through the Mirror). What finishes long after it was asked for (an
Operation, a job that fires at night) is kept with the world and granted the next time the player joins.

## To build

- **The era a player has reached** moves with the "Build a working ... computer" advancements: see [Eras](eras.md).
- **The Carbon front's advancements**, 50 more, in two new tabs and the four of today, for what the front
  brings: the chain, power and heat, Quince, the new systems, wireless and the portables, Teracoin, the
  buildings and the villagers. Afterwards:

  | Tab | Today | New | Then |
  | --- | ---: | ---: | ---: |
  | Fab | 0 | 14 | 14 |
  | Hardware | 14 | 10 | 24 |
  | Systems | 27 | 9 | 36 |
  | Networks | 15 | 7 | 22 |
  | Σ | 13 | 1 | 14 |
  | Teracoin | 0 | 9 | 9 |
  | All | 69 | 50 | 119 |

  **Fab** (new, the chain from the ore to the chip; its root is Some Assembly Required; a white concrete background):

  | Advancement | Description | Frame | After | Fires on | Icon |
  | --- | --- | --- | --- | --- | --- |
  | Some Assembly Required | Make a Vintage Electronics Workbench | task | none, the root | the Vintage Electronics Workbench made at the Crafting Table | Vintage Electronics Workbench |
  | Bare Metal | Form your first machine of the chain | task | Some Assembly Required | the first machine of the chain formed | Machine Casing |
  | Nine Nines | Grow polysilicon in the Siemens Reactor | task | Bare Metal | the Siemens Reactor's first polysilicon | polysilicon |
  | Pull Request | Pull your first silicon boule | task | Nine Nines | the Crystal Puller's first silicon boule | silicon boule |
  | Wafer-Thin | Cut a boule into wafers | task | Pull Request | the Wafer Saw's first wafers | silicon wafer |
  | Tape-Out | Write your first mask | task | Wafer-Thin | the first mask written (from a Layout in the Vintage, by the computer after) | a Vintage processor mask |
  | First Silicon | Package your first chip | goal | Tape-Out | the Packaging Line's first chip | a Vintage processor chip |
  | Bunny Suit | Build a Lithography Station | task | Bare Metal | the first Lithography Station formed | Clean Room Casing |
  | Die Shrink | Expose a layer in the Lithography Station of every era | challenge | Bunny Suit | a layer exposed in the Lithography Station of each of the five eras | an Advanced finished wafer |
  | 13.5 Nanometres | Expose a layer with extreme ultraviolet light | goal | Bunny Suit | a layer exposed in the Advanced station | an Advanced processor mask |
  | Burn-In | Make a screen on the Display Line | task | Bare Metal | the Display Line's first screen | tube screen |
  | Pick and Place | Let an Electronics Assembly Line make a part by itself | goal | First Silicon | an Electronics Assembly Line's first part | Vintage populated board |
  | Plug and Pray | Build a computer in the Prebuilder | task | Pick and Place | the Prebuilder's first computer | Vintage Personal Computer |
  | Do Not Turn Off Your Computer | Flash a motherboard's firmware in the Preinstaller | task | Plug and Pray | the Preinstaller's first firmware | MF Baby-AT I Motherboard |

  **Hardware**:

  | Advancement | Description | Frame | After | Fires on | Icon |
  | --- | --- | --- | --- | --- | --- |
  | The Megahertz Myth | Build a working Transition computer | task | Party Like It's 1999 | the first working Transition computer | Transition Personal Computer |
  | Hyperthreaded | Build a working Advanced computer | task | Moore's Law Isn't Dead | the first working Advanced computer | Advanced Personal Computer |
  | Magic Smoke | Burn a power supply | task, hidden | PC Master Race | one of the player's power supplies fails or burns | a failed power supply |
  | CPU Over Temperature | Trip a computer's thermal protection | task, hidden | PC Master Race | one of the player's computers trips at 100 °C | fire charge |
  | Uninterruptible | Ride out a power loss on a UPS | task | PC Master Race | one of the player's computers stays on through a power loss on a UPS's battery | MF PowerGuard 800 |
  | A Pea-Sized Dot | Fit a cooler with thermal paste | task | PC Master Race | a cooler with thermal paste fitted over a processor | thermal paste |
  | RGB Makes It Faster | Fill every fan slot of a case with Advanced case fans | goal | A Pea-Sized Dot | every fan slot of a case holding an Advanced case fan | Advanced case fan |
  | N+1 | Keep a server running when one unit of its redundant power supply fails | goal | Rack 'Em Up | one unit of a redundant power supply failing while its server stays on | MF ServerPSU 3000P |
  | It Just Works | Build a working Quince machine | task | PC Master Race | the first working Quince machine | Russet 128K |
  | Hard Copy | Print a book on the Large Scale Printer | task | PC Master Race | the Large Scale Printer's first book | book |

  **Systems**:

  | Advancement | Description | Frame | After | Fires on | Icon |
  | --- | --- | --- | --- | --- | --- |
  | hello | Start Quince System on a Russet 128K | task | Operating Systems | Quince System starting for the first time | Russet 128K |
  | Good Enough to Lick | Install Quince OS X | task | hello | Quince OS X installed | prismarine crystals |
  | Only Two Remote Holes | Install OpenBSD | task | It's Not Linux, Stop Asking | OpenBSD installed | pufferfish (OpenBSD's Puffy) |
  | Mint Condition | Install Linux Mint | task | The Universal Operating System | Linux Mint installed | DVD-RW |
  | Slide to Unlock | Unlock a portable for the first time | task | Operating Systems | a portable unlocked for the first time | qPhone |
  | Not Properly Shut Down | Watch a system check its disk after a sudden shutdown | task, hidden | Press DEL to Enter Setup | the first boot that checks a disk after a sudden shutdown | a Vintage hard disk |
  | Abandonware | Install a system found in an abandoned building | task | Operating Systems | a system found in an abandoned building installed | cobweb |
  | Shrink-Wrapped | Buy a system or a program from a Programmer | task | Operating Systems | the first trade with a Programmer | emerald |
  | Just a Backup | Copy an installer disc onto a blank one | task | Shrink-Wrapped | an installer disc copied with Easy Disc Creator | CD-RW |

  **Networks**:

  | Advancement | Description | Frame | After | Fires on | Icon |
  | --- | --- | --- | --- | --- | --- |
  | Pretty Fly for a Wi-Fi | Join a computer or a portable to an access point | task | Series of Tubes | the first link through an access point | Legacy Wireless Access Point |
  | Remote Work | Request items from the network on a portable | task | Pretty Fly for a Wi-Fi | the first request from a portable's network app | Sunsung Nebula S10 |
  | Pics or It Didn't Happen | Take a photo on a portable and send it to a computer | task | Pretty Fly for a Wi-Fi | the first photo sent from a portable's gallery to a computer | qPhone X |
  | Data Mining | Find a vein with the prospecting app | task | Pretty Fly for a Wi-Fi | the prospecting app pointing at a vein | compass |
  | a/s/l? | Send a message on the Messenger | task | Series of Tubes | the first Messenger message | paper |
  | Thin Client | Boot a computer over the network | task | Series of Tubes | the first boot over the network | Ethernet Cable |
  | Information Superhighway | Join two networks through their WAN Gateways | goal | Networks & Operations | two WAN Gateways joining two networks | Standard WAN Gateway Computer |

  **Σ**:

  | Advancement | Description | Frame | After | Fires on | Icon |
  | --- | --- | --- | --- | --- | --- |
  | Rubber Duck Debugging | Stop a program at a breakpoint | task | Erm, What the Sigma? | the first breakpoint in Virtual Studio's debugger | feather |

  **Teracoin** (new; its root is Not Your Keys, Not Your Coins; a gold block background):

  | Advancement | Description | Frame | After | Fires on | Icon |
  | --- | --- | --- | --- | --- | --- |
  | Not Your Keys, Not Your Coins | Make a wallet in the Crypto Wallet | task | none, the root | the first wallet made | gold nugget |
  | Proof of Work | Find a block | goal | Not Your Keys, Not Your Coins | one of the player's miners finding a block | Transition Mining Computer |
  | Pool Party | Join a mining pool | task | Not Your Keys, Not Your Coins | one of the player's miners joining a pool | water bucket |
  | To the Moon | Fill every riser of a Mining Computer | goal | Proof of Work | every riser of a Mining Computer holding a card | firework rocket |
  | Application-Specific | Mine with an ASIC | goal | Proof of Work | one of the player's ASICs mining | Blockwright Forge S1 |
  | Pizza Day | Send TRC to another player | task | Not Your Keys, Not Your Coins | the first send to another player's address | pumpkin pie |
  | HODL | Hold 1,000 TRC in one wallet for seven days without sending any | challenge | Pizza Day | 1,000 TRC in one wallet for seven game days with nothing sent from it | diamond (diamond hands) |
  | You Wouldn't Download a Car | Print an item on a 3D printer | task | Not Your Keys, Not Your Coins | a 3D printer's first item | Prosa i3 MK3 |
  | The Halvening | Be there when the reward halves | task, hidden | Proof of Work | being on the server when the reward halves | clock |
