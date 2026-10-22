# Networks and Operations

The model of the series' data network and the framework of Operations, the requests a network carries out. They are in
J's Core, not in J's Computers, so that a machine of any mod can be a member of a network and add the kinds of work it
does, without depending on J's Computers; J's Computers is what runs the network.

## What exists today

Everything this page designs is built on the Core's side, as of October 2026: the data network's model, peripheral
links, the framework of Operations, machines of any mod on the network, state as tables, and telemetry as J's Space's
own line. How a mod uses it, with code: [Networks](../NETWORKS.md) and [Operations](../OPERATIONS.md). What is left is
J's Computers' side, under *Still to build* at the end.

## The design

### Machines of any mod on the network

A machine of any mod becomes a member of a network through the Core, never through J's Computers' classes: it declares
its category, the kinds of Operation it carries out, and what the network may read of it. This is how J's Industrial's
and J's Space's machines that become computers join the network when J's Computers is installed, while working through
their own screens without it: **the network gives a machine more; it never makes it work**.

### Kinds of Operation from every mod

Each mod registers the kinds of Operation its machines carry out: J's Computers its storage and crafting, J's Industrial
its accelerators, research and miners, J's Space its rockets, satellites, probes and cargo, and so on. They are
dispatched by the Mainframe like any other, with queues, priority and the log.

### State as tables

A mod declares **the state of its machines as tables** through the Core: a row per machine, a column per value it shows.
J's Computers' SELECT reads them, so a player asks the network about a reactor or a satellite the same way as about
storage, and no mod depends on another to be read.

### Telemetry leaves the peripheral links

The Core had three kinds of peripheral cable: computing, telemetry and industrial control. Telemetry became J's Space's
own line, a group of the devices it joins ([Cables and lines](cables-and-lines.md)); computing and industrial control
stay point-to-point links.

## Still to build

J's Computers' side, when it moves onto the Core: its SELECT reading the tables of state, its storage keeping every
state of matter, the network reading the world's events, and what an era or a tier reached sets off.
