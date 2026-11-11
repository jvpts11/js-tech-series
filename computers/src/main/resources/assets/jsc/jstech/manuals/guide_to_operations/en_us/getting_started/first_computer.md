---
section: getting_started
order: 2
icon: jsc:personal_computer
shows: [jsc:personal_computer, jsc:motherboard_atx_standard_lga1150, jsc:cpu_integra_centro_c5_4690k, jsc:ram_ddr3_8192, jsc:gpu_vertex_gtx_970, jsc:psu_650g, jsc:disk_ssd_1t, jsc:monitor, jsc:dock_station]
---
# Building your first computer

Building a computer is easier than it sounds. You place a case, put the parts in it one by one, and then you add a monitor.

Take a Personal Computer from the creative tab J's Computers - Standard and place it. Using the computer opens its window, which is the inside of the case, with one place for each part. A case on its own does nothing yet.

## The parts

1. Put a motherboard in. It is the board the other parts sit on, and the case takes only a board of its own era ([](jsc:motherboards)).
2. Put a processor in. Its socket must match the board's socket ([](jsc:processors)).
3. Put memory in. The board takes some kinds of memory and not others ([](jsc:memory)).
4. Put a graphics card in. Without one the monitor has no picture, unless the processor has graphics built in ([](jsc:graphics_cards)).
5. Put a disk in. The system is installed on it, and your files are kept on it ([](jsc:disks)).
6. Put a power supply in. It gives the parts their power, rated in watts (W) ([](jsc:power_supplies)).

A computer with no disk still switches on, and then it reports that no system can start ([](jsc:no_bootable_device)). A computer with no motherboard or no power supply has no build at all, so it stays off.

A part that does not fit does not go into its slot. A processor or a memory module must match the board's socket, era and memory generation. A graphics card must fit the board's bus.

A power supply with a fixed wattage refuses the build when the parts draw more watts than it gives. The switch goes on, but the computer does not run.

On a Personal Computer, the window only says that it is not a computer yet, and what it still needs. The assembly screen of a server lists each problem in its tooltip.

## The monitor

A computer with no screen is not much use. Place a Monitor ([](jsc:monitors)) right against the case, and it is joined to the computer. Lay a [Peripheral Cable](jsc:peripheral_cables) when the monitor has to stand further away.

```scene
{"balloons": [{"number": 1, "part": {"x": 14, "y": 0, "z": 15}, "text": "The Personal Computer"}, {"detail": "Press it to turn the computer on.", "number": 2, "part": {"box": [12, 9, 15, 14, 12, 16], "x": 14, "y": 0, "z": 15}, "text": "Power button"}, {"number": 3, "part": {"x": 16, "y": 0, "z": 15}, "text": "The Monitor"}, {"detail": "It joins the monitor to the computer when the monitor cannot stand against the case.", "number": 4, "part": {"x": 15, "y": 0, "z": 15}, "text": "The Peripheral Cable"}], "blocks": [{"data": "{Hardware:{Size:15,Items:[{Slot:0,id:\"jsc:motherboard_atx_standard_lga1150\",count:1},{Slot:1,id:\"jsc:cpu_integra_centro_c5_4690k\",count:1},{Slot:2,id:\"jsc:ram_ddr3_8192\",count:1},{Slot:3,id:\"jsc:ram_ddr3_8192\",count:1},{Slot:6,id:\"jsc:gpu_vertex_gtx_970\",count:1},{Slot:10,id:\"jsc:psu_650g\",count:1},{Slot:11,id:\"jsc:disk_ssd_1t\",count:1}]},ManualOn:1b,Lamps:1}", "state": "jsc:personal_computer[facing=south]", "x": 14, "y": 0, "z": 15}, {"data": "{Lit:1b,SwitchedOn:1b}", "state": "jsc:monitor[facing=north,button=true,lit=true,on=true]", "x": 16, "y": 0, "z": 15}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,4116,0,0,0,0,0]}", "state": "jscore:cable", "x": 15, "y": 0, "z": 15}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,40,0,0,0,0,0]}", "state": "jscore:cable", "x": 15, "y": 0, "z": 14}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,2072,0,0,0,0,0]}", "state": "jscore:cable", "x": 16, "y": 0, "z": 14}], "caption": "A Personal Computer with its Monitor, joined by a Peripheral Cable. Drag it to see the back.", "height": 120}
```

The Dock Station ([](jsc:dock_station)) reads USB sticks, and that is where a USB install medium goes. Join it to the computer like any other drive.

> [!DEFINE] Install medium
> The floppy, CD, DVD or USB stick a system is installed from. The installer is the program on it.

The system and its programs show on the monitor, not in the window. The front of the case has the power button ([](jsc:case_front)).

## Turning it on

Press the power button on the front of the case ([](jsc:case_front)). The monitor shows the start-up, and if no system is on any disk, the firmware waits for you ([](jsc:no_bootable_device)).

Once the computer runs, the monitor's power strip offers the buttons Shut down the PC and Restart the PC.

1. Put the install medium of Frames 10 in the Dock Station.
2. Turn the computer on, and watch the monitor ([](jsc:turning_on)).
3. Start the installer from the medium, and follow it ([](jsc:installing)).

## If something goes wrong

> [!PROBLEM] The window's tooltip says "Not a computer yet".
> A build needs a motherboard, a processor, memory and a power supply. The tooltip also names a disk: put one in as well, so the computer has a system to start from.

> [!PROBLEM] The switch goes on, but the computer does not run.
> The build is refused. Check the parts against each other: the socket, the era, the memory generation and the power ([](jsc:build_refusals)).

> [!PROBLEM] The monitor says "No computer found in range over a Peripheral Cable".
> Put the monitor right against the case, or lay a Peripheral Cable between them ([](jsc:peripheral_cables)).

> [!PROBLEM] The monitor says "The computer has no video output: install a graphics card".
> Put a graphics card in the case, or use a processor with graphics built in ([](jsc:graphics_cards)).

> [!PROBLEM] The monitor says "No signal - the computer is powered off".
> Press the power button on the front of the case ([](jsc:case_front)).

> [!PROBLEM] The computer beeps, and the monitor says that its parts do not make a computer.
> A part is missing or does not fit. Open the computer: its CHECKS list names what to fix ([](jsc:build_refusals)).

> [!PROBLEM] A processor or a memory module does not go into its slot.
> Check the board: the socket, the era and the memory generation must match ([](jsc:motherboards)).

> [!PROBLEM] The firmware reports that no system can start.
> No system is on a disk yet. Install one ([](jsc:installing)).

> [!SEE] jsc:turning_on, jsc:installing
