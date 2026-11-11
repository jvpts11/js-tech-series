---
section: hardware
order: 18
icon: jsc:motherboard_atx_standard_lga1150
covers_family: jsc:motherboards
---
# Motherboards

The motherboard is the board every other part sits on, like the skeleton of a real computer. It decides which processor, memory and cards the computer can take, and which case takes it.

Hover a board and its tooltip tells you the rest. It names the board's form factor, its socket, its memory slots and the kinds of memory it takes, its expansion slots and their bus, whether it has sound of its own, whether it drives a screen by itself, and its era.

> [!DEFINE] Form factor
> The size and mounting standard of a board. It decides which cases take the board.

```table
{"caption": "The lines of the tooltip", "heads": ["Line", "What it tells you"], "rows": [[{"literal": "<form factor> form factor"}, {"words": "The size of the board, which decides which case takes it."}], [{"literal": "<n>x <socket>  |  <n> RAM (<kinds>)  |  <n>x <bus>"}, {"words": "The processor slots and their socket. The memory slots and the kinds of memory it takes. The expansion slots and their bus."}], [{"literal": "On-board audio"}, {"words": "Boards from the Transition on have sound of their own."}], [{"literal": "Seats parts of its own era only"}, {"words": "The board takes parts of its own era only."}], [{"literal": "Has a screen output of its own, for its console"}, {"words": "A server board drives a monitor by itself, so a server built on it needs no graphics card and keeps its expansion slots for other cards. Every other board says it has none: a monitor on it needs a graphics card, or a processor with graphics."}], [{"literal": "Firmware: <kind>"}, {"words": "The firmware on the board, its era's: a text BIOS, a blue BIOS or a UEFI ([](jsc:firmware))."}], [{"literal": "No firmware on this board"}, {"words": "A board with nothing on it yet. A computer built on it turns its fans and nothing more, until a Preinstaller writes its firmware ([](jsc:computer_making_machines))."}]]}
```

Each computer takes only some boards. The table shows which ones, and the board's era must match the computer's era too.

Table: The boards each computer takes

| Computer | Era | Boards it takes |
| --- | --- | --- |
| Personal Computer | Vintage | Baby-AT and AT |
| Personal Computer | Legacy | ATX only |
| Personal Computer | Transition and later | ATX and EATX |
| Crafting Computer | Vintage | Baby-AT and AT |
| Crafting Computer | Legacy and later | ATX only |
| Cluster Management Computer | Vintage | Baby-AT and AT |
| Cluster Management Computer | Legacy and later | ATX and EATX |

The Mainframe takes only MTX boards. A board of another era is refused at the slot, without a word. A board of another form factor is refused the same way.

A cheaper board may have fewer memory slots or expansion slots than the case has room for.

The expansion slots run on a bus. The buses, from the oldest to the newest, are ISA, PCI, AGP 2x, AGP 4x, AGP 8x, and PCI-e 1.0, 2.0, 3.0, 4.0, 5.0 and 6.0. A card needs a bus of its board's family ([the bus family](jsc:build_refusals)).

A few server boards also take the cards of an older bus: the Vintage server board takes ISA cards beside its PCI ones, and the Legacy server boards take PCI cards beside their PCI-e ones. The board's tooltip names the older bus.

## If something goes wrong

> [!PROBLEM] It does not go into the case.
> The case takes only some form factors, and only boards of its own era. Check the table above.

> [!PROBLEM] A part sits in the window but does nothing.
> The board does not offer that slot. Use a board with more slots of that kind.

> [!PROBLEM] The computer switches on, its fans turn, and nothing more happens.
> Its board has no firmware: the window says NO FIRMWARE. A Preinstaller writes it ([](jsc:computer_making_machines)).

> [!PROBLEM] A card does not go into a server.
> The window shows only the expansion slots the board and the server's case wire, and a small server has only one or two, of which a graphics card takes one. Build the server on a server board, which has a screen output of its own, and its slots stay free for other cards.

> [!SEE] jsc:eras, jsc:processors, jsc:memory
