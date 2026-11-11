---
section: assembly
order: 15
icon: jsc:personal_computer
shows: [jsc:motherboard_atx_standard_lga1150, jsc:cpu_integra_centro_c5_4690k, jsc:ram_ddr3_8192, jsc:gpu_vertex_gtx_970, jsc:psu_650g]
---
# The assembly screen

A computer here is a case with parts inside it. Right-click the case to open the assembly screen. It shows the inside of the computer: a slot for each part, and the switches and lamps that run it. Sneak-click the side of the case, not one of its front parts, to take its left side panel off or put it back.

```scene
{"balloons": [{"detail": "Every other part sits on it, and it decides which parts fit.", "number": 1, "part": {"box": [1, 5, 0, 13, 15, 3], "x": 15, "y": 0, "z": 15}, "text": "The motherboard"}, {"detail": "The processor sits under the cooler, in the socket its board names.", "number": 2, "part": {"box": [4, 10, 2, 9, 15, 7], "x": 15, "y": 0, "z": 15}, "text": "The processor and its cooler"}, {"detail": "Two modules stand in their slots.", "number": 3, "part": {"box": [9, 9, 1, 13, 15, 4], "x": 15, "y": 0, "z": 15}, "text": "The memory"}, {"number": 4, "part": {"box": [1, 7, 1, 11, 10, 5], "x": 15, "y": 0, "z": 15}, "text": "The graphics card"}, {"number": 5, "part": {"box": [1, 0, 1, 13, 4, 9], "x": 15, "y": 0, "z": 15}, "text": "The power supply"}, {"number": 6, "part": {"box": [9, 0, 6, 14, 2, 11], "x": 15, "y": 0, "z": 15}, "text": "The hard disk"}, {"detail": "It sits on the board itself, not in a bay.", "number": 7, "part": {"box": [8, 6, 1, 12, 8, 3], "x": 15, "y": 0, "z": 15}, "text": "The M.2 drive"}, {"detail": "It sits behind the front of the case.", "number": 8, "part": {"box": [7, 12, 3, 16, 15, 13], "x": 15, "y": 0, "z": 15}, "text": "The DVD drive in the bay"}], "blocks": [{"data": "{Hardware:{Size:15,Items:[{Slot:0,id:\"jsc:motherboard_atx_standard_lga1150\",count:1},{Slot:1,id:\"jsc:cpu_integra_centro_c5_4690k\",count:1},{Slot:2,id:\"jsc:ram_ddr3_8192\",count:1},{Slot:3,id:\"jsc:ram_ddr3_8192\",count:1},{Slot:6,id:\"jsc:gpu_vertex_gtx_970\",count:1},{Slot:10,id:\"jsc:psu_650g\",count:1},{Slot:11,id:\"jsc:disk_hdd_4t\",count:1},{Slot:12,id:\"jsc:disk_nvme_500g\",count:1},{Slot:13,id:\"jsc:internal_dvd_drive\",count:1}]},SidePanelOff:1b}", "state": "jsc:personal_computer[facing=east]", "x": 15, "y": 0, "z": 15}], "caption": "A Personal Computer with its side panel off and every part in its seat. Drag it to turn the case.", "height": 130, "view": {"pitch": 20.0, "yaw": -20.0, "zoom": 1.2}}
```

A computer runs only when its parts make a build that stands. You read the state of the build in the header of the window, and the window shows each part in its own slot.

The POWER button's tooltip names the parts a computer needs. Without a graphics card the monitor stays dark, unless the processor has graphics built in.

> [!DEFINE] Build
> The set of parts in the slots of the assembly screen. It is valid when it has no problems.

> [!DEFINE] Motherboard
> The board every other part sits on. It decides which parts the computer can take.

> [!DEFINE] Processor (CPU)
> The part that does the computing. CPU stands for central processing unit.

> [!DEFINE] Memory (RAM)
> Where a computer keeps what it works on right now. RAM stands for random-access memory.

> [!DEFINE] Graphics card (GPU)
> The part that draws the picture for the monitors. GPU stands for graphics processing unit.

> [!DEFINE] Power supply (PSU)
> The part that gives power to the other parts. PSU stands for power supply unit.

> [!DEFINE] Socket
> The place on the board where a processor sits. A processor fits only the socket its board names.

> [!DEFINE] RAM buffer
> How many items a computer can hold while it moves them: the total buffer of its memory modules.

## Reading the window

The title in the header tells you which computer you are looking at.

Table: Title in the header

| Title | Computer |
| --- | --- |
| `PC` | A Personal Computer. |
| `CC` | A Crafting Computer. |
| `CMC` | A Cluster Management Computer (CMC). |
| `MAINFRAME` | A Mainframe. |
| `SERVER` | A server in a Server Rack. |
| `SUPERCOMPUTER NODE` | A supercomputer node in a Server Rack. |

The word in the top right tells you the state of the computer.

Table: The state word

| Word | Colour | What it means |
| --- | --- | --- |
| `OFFLINE` | Red | The build is not valid: there is no build, or it has problems. |
| `READY` | Amber | The build is valid, and the computer is switched off. |
| `ONLINE` | Green | The build is valid, and the computer is switched on and runs. |
| `NO FIRMWARE` | Amber | The computer is switched on, but its board has no firmware: its fans turn and nothing more ([](jsc:motherboards)). |

Three lamps show what the computer is doing.

Table: The lamps

| Lamp | Lit when |
| --- | --- |
| `PWR` | Green while the computer runs. Red while it does not. |
| `DISK` | Amber while the computer works its disk. |
| `NET` | In the accent colour while the computer is linked to a network. |

A tower has readouts that show its numbers. Hover one to read its tooltip.

Table: The readouts of a tower

| Readout | What it shows |
| --- | --- |
| `CAPACITY` | The items per tick (it/t) the computer handles, lit while it runs. |
| `RAM BUFFER` | The buffer of its memory, in items (it). |
| `NETWORK` | LINKED in the accent colour on a network, otherwise a dim "--"; the tooltip counts the linked servers. |

## The switches and the power button

The large button on the left of a tower's front panel is POWER. It switches the computer on and off. A tower has two switches.

Table: The switches of a tower

| Switch | What it does |
| --- | --- |
| `AUTO START` | Keeps the computer on while its build stands (see below). |
| `SIDE PANEL` | Lit while the side panel is off, so you can see the parts. |

Switch AUTO START on while the build is valid, and the computer turns on at once. It stays on as long as the build stands. While AUTO START is on, POWER is locked, and its tooltip says: "Auto start keeps it on: switch auto start off to switch it off here".

If the build stops standing, the computer goes dark. When the build stands again with AUTO START on, the computer comes back on by itself.

If you switch the power on with an invalid build, the switch goes on, but the computer does not run. The board then tests itself and beeps its failure.

While the build is not valid, hovering POWER shows three lines: "Not a computer yet", then "Needs a board, a CPU, RAM, a disk and a PSU", then "Then press to switch on". The middle line is a general reminder, so it does not name the part that is wrong.

The readouts and the POWER button show their tooltip only when the mouse rests on them and the cursor holds nothing.

The screen of a Mainframe has AUTO START and FAILOVER. Read about failover in [Failover](jsc:failover) and about the Mainframe in [Mainframes](jsc:mainframes).

## Naming the computer

The name field in the header names the computer. It takes up to 32 characters, and it saves each change as you type. An empty field shows a hint that invites you to name the computer.

## Taking a computer with you

Break a computer and it comes back to you whole: one item with every part in its seat, and its name. Its tooltip lists the parts inside it, and what it still lacks to run. Place it again and it stands as it was. Only the media in its front drives, the discs and the floppies, come out on their own.

## To make a computer run

1. Right-click the case to open the assembly screen.
2. Put the parts into their slots, starting with the motherboard.
3. Read the word in the header: READY means the build is valid and the computer is off.
4. Press POWER, or switch AUTO START on to keep the computer on.

## If something goes wrong

> [!PROBLEM] The POWER button does nothing.
> AUTO START is on, and it locks the button. Switch AUTO START off first.

> [!PROBLEM] The switch is on, but the computer stays off.
> The build is not valid. Hover POWER: its three lines name the parts a computer needs. On a rack server, the CHECKS list names each problem ([When the build is refused](jsc:build_refusals)).

> [!PROBLEM] A part sits in the window and does nothing.
> The board does not offer that slot. Use a board with more slots ([](jsc:motherboards)).

> [!SEE] jsc:first_computer, jsc:case_front, jsc:build_refusals
