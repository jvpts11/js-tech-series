---
section: fab
order: 43
icon: jsc:mould_blank
covers: [jsc:mould_blank]
---
# Cutting a mould

Each socket, slot and connector of a board is made of its own plastic body, each case fan is built on its own frame and rotor, and each case, Personal Computer, monitor, mainframe and server rack takes its own bezel, as each liquid cooler takes its pump cover; the Injection Moulder moulds every one in its own steel mould. It also sheathes each cable in its own jacket, extruded through the cable's own die, a steel tool cut the same way. And each form factor of motherboard, and each of Quince's logic boards, is assembled on its own board jig, a steel fixture cut here too and lent to the job that assembles the board, at the Electronics Assembly Line or the bench, where it stays in its tray. A mould is never used up: a job lends it to the moulder and takes it back, and one mould serves every body of its part. Every mould is cut on a Mould Blank, four steel plates made into one at the Electronics Workbench.

You cut the mould from the computer linked to the moulder, with Midsoft Lithography, the program that writes a Lithography Station's masks.

1. Install Midsoft Lithography on the computer linked to the Peripheral Port behind the moulder's tower, from its install disc.
2. Put a Mould Blank in the moulder's mould slot.
3. Open the program: type litho at the prompt of MC-DOS or MC-NET, or open Lithography on the desktop.
4. Pick the family, then the mould: with the arrow keys and [[Tab]] on the text screen, with the tabs and the list in the window.
5. Press [[W]] on the text screen, or Cut Mould in the window.

The moulder cuts the mould in one step's time, 10 seconds, and the computer holds 8 MB of its memory while it cuts. Any running computer cuts any mould. The mould then stands where the blank was, and the moulder moulds its body in it.

A moulder cuts no mould while it moulds a part: cut the mould when the part leaves.

```table
{"caption": "The program's lists", "heads": ["List", "Its moulds"], "rows": [[{"words": "Sockets"}, {"words": "the body of each processor socket"}], [{"words": "Slots"}, {"words": "the body of each memory slot and expansion slot"}], [{"words": "Connectors"}, {"words": "the body of each storage and power connector"}], [{"words": "Fans"}, {"words": "each era's fan frame"}], [{"words": "Bezels"}, {"words": "the bezel of each case, Personal Computer, monitor, mainframe and server rack and of the floppy drive, the liquid coolers' pump covers and the robot arm's cover"}], [{"words": "Cables"}, {"words": "the extrusion die of each cable's jacket"}], [{"words": "Jigs"}, {"words": "the jig of each form factor of motherboard and of each Quince logic board"}]]}
```

```command
id: jsc:command/mc_net/litho
```

## If something goes wrong

> [!PROBLEM] The program says no Lithography Station is linked to this computer.
> Lay a peripheral cable from the Peripheral Port behind the moulder's tower to this computer: the program shows a linked moulder as it shows a station.

> [!PROBLEM] It says to put a Mould Blank in the moulder.
> The moulder holds no blank to cut. Put a Mould Blank in its mould slot, then cut the mould again.

> [!PROBLEM] It says the moulder is moulding.
> The moulder is moulding a part in the mould it holds. Wait for the part to leave, take the mould out, put a blank in, and cut the new mould.

> [!SEE] jsc:board_machines, jsc:mask_writing
