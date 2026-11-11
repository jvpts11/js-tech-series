---
section: midsoft_installs
order: 202
icon: jsc:monitor
---
# Memory and the Frames editions

Setup does not refuse a machine for having little memory. Its check page shows your Memory line, and a machine with little memory can still take a Frames install. It then runs tight and boots slowly, as the section on tight RAM explains below.

The system keeps its own RAM first, before anything else: 32 MB for Frames 95, 128 MB for XP, 1 GB for 7, 3 GB for 10 and 8 GB for Frames 11 ([](jsc:frames_which_machine)). Every window you open takes memory too: its program's own RAM.

What a program holds grows with the era of the machine it runs on, because software grew with the machines it was written for. The figure a program gives is what it holds on a Legacy machine. It holds a sixteenth of that on a Vintage machine, 8 times as much on a Transition machine, 32 times on a Standard machine and 128 times on an Advanced machine. A program that comes with the system holds 16 MB on a Legacy machine. A typical machine of each era, with two memory sticks of the middle size, holds its system and about eight programs.

Free memory is the machine's total minus everything held. When a program needs more than is free, every Frames edition crashes: it does not refuse the program. This is true for a window, for a program started at the prompt and for a service. The newer editions warn first, as the table shows.

Table: What happens as the memory fills

| Edition | Kernel | What happens |
| --- | --- | --- |
| Frames 95 | Cooperative | The system crashes, with no warning. |
| Frames XP, 7, 10 and 11 | Preemptive | A balloon warns once the memory passes nine tenths. Then the system crashes on the program that does not fit. |

## The warning

On Frames XP, 7, 10 and 11, the window that takes the memory past nine tenths opens with a balloon that says "Low on memory". Its text asks you to close some programs, and names the three that hold the most, with what each holds.

## The crash

Each edition goes down on its own page. Frames 95 shows "A fatal exception has occurred.". XP and 7 show a stop error named MEMORY_MANAGEMENT. Frames 10 and 11 show a sad face, a code you can scan, and the stop code MEMORY_MANAGEMENT.

The page is on every monitor that shows the computer, and keys and clicks do nothing while it stands. The programs the computer ran stop. The page stays for 4.2 seconds on Frames 95, 6 seconds on XP, 8 seconds on 7 and 10 seconds on 10 and 11. Then the computer starts over from its self-test, into an empty session.

## The red page

A part taken out of a running computer brings the system down too: a stick of memory, an expansion card, or the disk the system runs from. Frames shows the same page in red, and Frames 10 and 11 name the part. If the computer still has what it needs to run, it starts over after the page. If it does not, the monitor goes dark. Taking out the processor, the board or the power supply switches the computer off at once, with no page.

Graphics programs also need video memory. When a window needs more than is free, the balloon says "Not enough video memory" and asks you to turn off a monitor or close a graphics program.

## Tight RAM and the boot time

A machine is tight for a system when it holds less than twice the system's own RAM. A tight boot takes one and a half times as long. You can see the limit for each edition in the table.

```table
{"caption": "Memory below which each edition is tight", "heads": ["Edition", "Tight below"], "rows": [[{"words": "Frames 95"}, {"amount": 64, "unit": "MB"}], [{"words": "Frames XP"}, {"amount": 256, "unit": "MB"}], [{"words": "Frames 7"}, {"amount": 2048, "unit": "MB"}], [{"words": "Frames 10"}, {"amount": 6144, "unit": "MB"}], [{"words": "Frames 11"}, {"amount": 16384, "unit": "MB"}]]}
```

A boot takes about one second, plus the system's footprint in megabytes divided by (160 times the disk's speed times the machine's era factor). The disk's speed is 1 for a hard disk, 4 for a solid-state drive (SSD) and 16 for an NVMe drive. The era factor is 2 for Legacy, 3 for Transition, 4 for Standard and 8 for Advanced. The result is at least 2 seconds and at most 20 seconds.

With enough RAM, on a hard disk, Frames XP on a Legacy machine boots in about 6 seconds, and Frames 11 on an Advanced machine in about 17 seconds. Frames 7 on a Transition machine and Frames 10 on a Standard machine reach the 20-second limit.

> [!DEFINE] Kernel
> The part of the system that runs the programs and switches between them.

> [!DEFINE] Cooperative kernel
> A kernel in which each program must give the processor back by itself, because nothing forces it to.

> [!DEFINE] Preemptive kernel
> A kernel that switches programs by force, so that one program cannot hold the processor.

> [!DEFINE] Balloon
> The small notice that appears in the notification area of the desktop.

> [!DEFINE] Tight RAM
> The state of a machine that holds less than twice its system's own RAM.

## If something goes wrong

> [!PROBLEM] The computer reboots after a blue crash page.
> A program needed more memory than was free. Close a program before you open another one, or add RAM ([](jsc:memory)).

> [!PROBLEM] A balloon says "Low on memory".
> The memory is past nine tenths. Close a program, or add RAM to the computer ([](jsc:memory)).

> [!PROBLEM] The crash page is red.
> A part was taken out while the computer ran. Put it back, and switch the computer on again if its monitor went dark.

> [!PROBLEM] A balloon says "Not enough video memory".
> Turn off a monitor, or close a graphics program.

> [!PROBLEM] The computer takes a long time to start.
> The machine is tight on RAM. Add RAM until it holds at least twice the system's own RAM, using the table above.

> [!SEE] jsc:memory, jsc:frames_which_machine
