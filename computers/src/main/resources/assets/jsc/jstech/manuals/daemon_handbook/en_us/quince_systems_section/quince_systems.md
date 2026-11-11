---
section: quince_systems_section
order: 355
---
# Quince's systems

Quince makes its own computers and its own systems for them. Each system runs on the processors it was built for, and on no other: you cannot install a system for the PowerPC on an Integra processor. Quince's computers are described in [](jsc:quince_computers).

> [!DEFINE] Quince System
> Quince's first system, for the Russet 128K and its 68k processor.

> [!DEFINE] Quince OS 9
> The last classic system, for the PowerPC of the PowerQ G3 and G4.

> [!DEFINE] Quince OS X
> The system that came after, for the PowerPC and for Integra processors.

> [!DEFINE] Quince OS X Server
> Quince OS X for servers, with Server Admin to run their services.

> [!DEFINE] quinceOS
> Quince's newest system, for the Q1, Q2 and Q3, Quince's own chips.

Table: The systems

| System | First era | Processors | Disk | Memory |
| --- | --- | --- | --- | --- |
| Quince System | Vintage | 68k | 1 MB | 1 MB |
| Quince OS 9 | Legacy | PowerPC | 400 MB | 32 MB |
| Quince OS X | Legacy | PowerPC, x86-64 | 2,048 MB | 128 MB |
| Quince OS X Server | Transition | x86-64, ARM64 | 4,096 MB | 256 MB |
| quinceOS | Advanced | ARM64 | 16,384 MB | 4,096 MB |

The Quince System and Quince OS 9 are the classic systems. They run on the kernel quince_classic, which lets one program run until it gives way to the next, and they have no command line at all: everything is done with the mouse. Quince OS X, its Server and quinceOS run on the kernel wallace, a UNIX underneath, with a Terminal and the zsh shell. That UNIX is why Quince's chapter is in this handbook.

> [!DEFINE] Classic system
> The Quince System or Quince OS 9: no command line, and memory given out in fixed blocks.

Quince OS X carries Champollion, which runs a program built for the PowerPC on an x86-64 processor at half its speed. Quince OS X Server and quinceOS carry Champollion 2, which runs x86-64 programs on Quince's chips at four fifths of their speed ([](jsc:processors)).

Table: How each formats its disk

| System | File system |
| --- | --- |
| Quince System | HFS |
| Quince OS 9 | HFS+ |
| Quince OS X and its Server | HFS+ (Journaled) |
| quinceOS | APFS |

## If something goes wrong

> [!PROBLEM] The installer does not offer the system.
> The computer's processor is not one the system was built for. Install it on a Quince computer with the right processor.

> [!SEE] jsc:the_systems, jsc:quince_installing, jsc:quince_desktop
