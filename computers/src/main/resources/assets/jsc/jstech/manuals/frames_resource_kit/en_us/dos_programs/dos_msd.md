---
section: dos_programs
order: 207
---
# MSD and its keys

Midsoft Diagnostics (MSD) reads the hardware of the computer and shows what it is made of. Type msd at the prompt to open it. The program takes the whole screen, and it works on MC-DOS and on [MC-NET](jsc:mc_net).

1. Type msd at the prompt and press [[Enter]].
2. Move the pick with the arrow keys. Press [[Enter]] on LPT Ports or COM Ports to open a list.
3. In the list, pick a port with the arrows, then press [[E]] to enable its device or [[D]] to disable it.
4. Press [[Enter]] or [[Esc]] to put the list away. Press [[F3]] on the main screen to give the terminal back.

Table: The buttons of the main screen

| Button | What it shows |
| --- | --- |
| Computer... | The name of the first processor. |
| Memory... | The conventional memory, 640K, then the memory above 1 MB, counted in K. |
| Video... | The name of the first video card, or (none). |
| Network... | The cable of the first network adapter, or (none). |
| OS Version... | The name of the system. |
| Disk Drives... | The drive letters the machine sees, such as C: D:. |
| LPT Ports... | The number of parallel ports. Zero when there are none. Opens a list. |
| COM Ports... | The number of serial ports. Opens a list. |

Only LPT Ports and COM Ports open a list. The other buttons show what they found, and Enter does nothing on them.

Table: The columns of a ports list

| Column | What it shows |
| --- | --- |
| Port | The name of the port, such as COM1 or LPT1. |
| Attached | The device on the port, or (none) when nothing is attached. |
| Status | On, or Off (disabled) for a device you have disabled. |

Table: The keys

| Screen | Key | What it does |
| --- | --- | --- |
| Main screen | `Arrows` | Move the pick between the buttons. |
| Main screen | `Enter` | Opens the button picked. |
| Main screen | `L` | Opens the LPT ports. |
| Main screen | `C` | Opens the COM ports. |
| Main screen | `Esc or F3` | Leaves the program. |
| Ports list | `Up and Down` | Pick a port. |
| Ports list | `E` | Enables the device of the port picked. |
| Ports list | `D` | Disables the device of the port picked, which keeps its port while the computer neither reads nor writes it. |
| Ports list | `Enter or Esc` | Puts the list away. |

A mouse click works too: on a button, on a port, and on the Enable and Disable buttons under the list.

> [!DEFINE] Midsoft Diagnostics (MSD)
> The program that reads the hardware of the computer and reports what it finds.

> [!DEFINE] LPT port
> A parallel port, where a printer is usually plugged in.

> [!DEFINE] COM port
> A serial port, where a modem or a mouse of the era could be plugged in.

```command
id: jsc:command/mc_dos/msd
```

DEFRAG takes the whole screen the same way, to repair a corrupted disk ([](jsc:corruption_and_the_defrag)).

```command
id: jsc:command/mc_dos/defrag
```

## If something goes wrong

> [!PROBLEM] The screen says msd: there is no machine to read.
> msd has no computer to read. Type msd at the prompt of a computer.

> [!PROBLEM] A port says (none) under Attached.
> Nothing is plugged into that port, so there is no device to enable or disable.

> [!PROBLEM] A port says Off (disabled) under Status.
> You disabled its device. Pick the port and press [[E]] to enable the device again.

> [!SEE] jsc:printers, jsc:dos_help
