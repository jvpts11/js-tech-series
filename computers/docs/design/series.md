# The series

The J's Tech Series is one mod per subject, on top of J's Core, a general-purpose library for technology mods with no
gameplay of its own. J's Computers is one of those mods. This page says what it gives the others and what it expects
from them.

## Who depends on whom

**Every mod of the series depends only on J's Core, and on no other mod of the series.**

- J's Core ← J's Computers
- J's Core ← J's Industrial
- J's Core ← J's Space, J's Warfare, J's Transport, J's Agriculture, J's Civil Works, J's Robotics, J's Geology, J's
  Oceanics, each on its own
- J's Overworld (the Overworld's generation) stays outside the series and also depends only on J's Core.

So J's Computers never depends on J's Industrial or on any other mod of the series, and none of them depends on J's
Computers. Each one works installed alone with J's Core, in a modpack that has none of the others.

**How the mods work together** when several are installed: through what J's Core gives them all (the model of the
network, the Operations framework, the capabilities, tags and its event bus), and through integrations that only wake
when the other mod is present, kept behind a guard like any optional mod's, so that its absence never breaks anything.
Machines expose J's Core's capabilities, and J's Computers drives them through those. No mod holds another's internal
registries. What a mod adds to J's Computers through J's Computers' API ([The API](api.md)) is one of those optional
integrations.

## Other mods' Operations

A mod registers its Operation types through `CoreRegisterEvent` ([Operations](operations.md), [The API](api.md)). The
Mainframe dispatches them like any other, with queues, priority, logging and provenance; the logic (ballistics, orbits,
harvests) belongs to the mod that owns them. They show in the Network Manager and in the log with no special treatment,
and a Σ# program submits them like the others.

## What already serves any mod, with no code

- The External Storage Bus uses any inventory as the network's storage ([Storage](storage.md)).
- The Import Bus and the Export Bus fill and empty any inventory.
- The Crafting Interface and the Crafting Input Routers feed any machine with slots, with machine recipes written in the
  Pattern Studio ([Autocrafting](autocrafting.md)).
- Another mod's chemicals come in as the third kind of data when the bridge exists.

## Hardware and recipes

Today everything comes from the creative tab. J's Computers makes every part of every era by its own simple path, from
vanilla materials and J's Core's catalogue, and with J's Industrial installed the industrial chain is the path at scale;
a server setting can make it the only one ([Eras](eras.md)).

## The other mods

- **J's Industrial:** its machines join the network through the capabilities, and its cables live in J's Core's cable
  block ([The network](network.md)). With both mods installed, J's Industrial is among the first users of the API's
  computer and hardware registries ([The API](api.md)): it adds the **Industrial Controller Computer** (the ICC, one per
  era) and its **Fieldbus Cards**, the only computer that reaches J's Industrial's machines over industrial control
  cable; it lets the machines it declares (accelerators, instruments, the Computational Research Centre and others) take
  server hardware and run as computers, writing their data as files on the network; it adds a **Research Router**, a
  topology element as the Server Router, and a **Research** tab to the Cluster Manager
  ([Servers and racks](servers-and-racks.md)); and it registers its own Operations and industrial programs. J's
  Computers' Pattern Studio and Crafting Manager show every recipe, marking the ones the team hasn't researched with the
  discovery they need. The simulator's materials are J's Industrial's exotic elements
  ([The cosmological simulator](simulator.md)), and the Singularity era's hardware is made of its computronium
  ([Eras](eras.md)). The energy computers draw is J's Core's, which J's Industrial generates and carries
  ([Power](power.md)).
- **J's Space:** the satellites are all J's Space's: the Satellite Control Computer and its specialised hardware (the
  Satellite Cards), the Satellite Manager program, the Satellite Antenna, the satellites, launching, and the dedicated
  infrastructure that moves and handles satellite data, the **telemetry** system (cables and devices of its own, because
  a satellite sends an enormous amount of data). When both mods are installed, J's Computers gives it:
  - **the network:** the satellites' Operations (associating, dissociating, state, commands) are types J's Space
    registers, and the Mainframe dispatches them like any other, with queues, priority and logging;
  - **the computer:** the Satellite Control Computer is a network computer like J's Computers' own, with a motherboard,
    processors, memory, a power supply and a system; J's Space adds it, and the Satellite Cards, through the API's
    computer and hardware registries ([The API](api.md));
  - **the program:** the Satellite Manager registers like any program ([Programs](programs.md));
  - telemetry is the fourth of the five cable systems, now J's Space's, which registers its cables in J's Core's cable
    block; it is not a sixth.

  The links to other systems (Communication Satellites and wireless, Mapping Satellites, the Laser Satellite's shot and
  security) are designed with J's Space.

  **Enigmatic hardware:** with J's Space installed, its research system unlocks Enigmatic hardware for computers
  ([Hardware](hardware.md)). J's Space adds it, through the API's hardware registry.
- **J's Overworld:** with J's Computers installed, its structures (abandoned labs) can hold pre-trained AI models
  ([Artificial intelligence](ai.md)).
- **J's Robotics:** with J's Computers installed, it trains robots' behaviours on the AI's infrastructure
  ([Artificial intelligence](ai.md)).
- **J's Economy:** Teracoin is meant to be its currency when both mods are installed ([Teracoin](teracoin.md)).

What each of the other mods does with J's Computers (its Operations, its programs, its hardware) is designed with that
mod, always as something that works when both are installed and is simply absent when J's Computers is not.
