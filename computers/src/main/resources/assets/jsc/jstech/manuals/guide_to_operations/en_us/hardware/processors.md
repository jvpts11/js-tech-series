---
section: hardware
order: 19
icon: jsc:cpu_integra_centro_c5_4690k
covers_family: jsc:processors
---
# Processors

The processor, or CPU, does the actual computing. Every program you run is the processor following its instructions, one after the other, very fast.

Two numbers say how much a processor can do: its cores, and its clock speed in GHz. Your processor's cores are the units of work that run at the same time.

> [!DEFINE] Core
> One unit of work a processor does at the same time as its other cores.

> [!DEFINE] Clock speed
> How fast each core runs, in GHz (gigahertz): billions of beats of the clock a second.

Hover a processor and its tooltip shows its capacity, in items a tick (it/t), and its power in watts. On a Mainframe, the capacity of its processors adds up to how many items a tick the whole network handles ([Mainframes](jsc:mainframes)).

A processor also has to fit the board. Its socket must be the board's socket, and its era must be the board's era. With no board in place yet, a processor goes into any slot, so you can set up your parts first.

Every processor belongs to an instruction set. A program built for one set runs on it and on every newer set of its line, never on an older one and never on another line. Processors of two sets never share a computer, and a system built for one set is not installed on a processor of another.

Table: The instruction sets

| Instruction set | Word size | Runs programs built for |
| --- | --- | --- |
| IA-16 | 16 bits | IA-16 |
| x86 | 32 bits | x86 and IA-16 |
| x86-64 | 64 bits | x86-64, x86 and IA-16 |
| 68k | 32 bits | 68k |
| PowerPC | 32 bits | PowerPC |
| ARM64 | 64 bits | ARM64 |

The last three are Quince's. The Motoroller 68000, the PowerQ G3 and G4 and the Quince Q1 to Q3 go only on Quince's logic boards. The Q chips carry their memory in the package, shared with their graphics, so a machine runs on one with no memory module.

A program crosses from one line to another when it is built for both: sigma build --isa powerpc,x86-64 makes a universal build, which runs on each ([](jsc:sigma_compile)). A system can also carry a translator, which runs it more slowly: Champollion runs PowerPC programs on x86-64 processors at half their speed, and Champollion 2 runs x86-64 programs on Quince's own chips at four fifths of it.

The word size of a processor depends on its era, and so does the size of one item. You will see the same sizes on the disks ([Units and ticks](jsc:units_and_ticks)).

Table: Word size and item size by era

| Era | Word size | Size of one item |
| --- | --- | --- |
| Vintage | 16 bits | 1 MB |
| Legacy | 32 bits | 16 MB |
| Transition | 64 bits | 256 MB |
| Standard | 64 bits | 256 MB |
| Advanced | 64 bits | 256 MB |
| Exa | 64 bits | 256 MB |
| Singularity | 64 bits | 256 MB |

There are more than 170 processors in the mod, from the Integra and Velocion makers and from Quince. A processor with graphics built in says so on its tooltip, and it lights a monitor without a graphics card.

## If something goes wrong

> [!PROBLEM] It will not go in.
> Its socket or its era is not the board's. Use a processor with the board's socket and era.

> [!PROBLEM] The build says the CPU instruction set does not match.
> Use processors of one instruction set.

> [!PROBLEM] A system will not install, or the firmware finds nothing to start on a disk.
> The system is built for another processor. Use a system built for this machine's.

> [!PROBLEM] The build says there are too many CPUs.
> Take processors out, or use a board with more CPU slots.

> [!SEE] jsc:motherboards, jsc:mainframes, jsc:eras
