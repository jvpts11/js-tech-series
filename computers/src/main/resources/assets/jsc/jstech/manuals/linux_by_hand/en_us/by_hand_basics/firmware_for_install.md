---
section: by_hand_basics
order: 301
icon: jsc:motherboard_atx_standard_lga1150
---
# BIOS or UEFI

The firmware comes in two kinds, and the era of your motherboard decides which one you have. The older kind is the basic input/output system (BIOS). The newer kind is the Unified Extensible Firmware Interface (UEFI). The install needs the difference, because each kind starts a disk in its own way.

> [!DEFINE] BIOS
> The older kind of firmware, in text or with a blue panel. It starts a disk from the very first part of it.

> [!DEFINE] Partition
> A part of a disk that the system treats as a disk of its own. One disk can hold several of these parts.

> [!DEFINE] UEFI
> The newer kind of firmware. It reads a small partition of the disk, formatted as FAT (File Allocation Table), to find the program that starts the system.

Table: The firmware of each era

| Era | Firmware | What the install needs |
| --- | --- | --- |
| Vintage | Text BIOS | The whole disk: the start program goes on the disk itself. |
| Legacy and Transition | Blue BIOS | The whole disk: the start program goes on the disk itself. |
| Standard, Advanced and later | UEFI | A small FAT partition for the start program, made in the disk steps. |

On a Vintage, Legacy or Transition computer the walkthrough is shorter. Its opening lines say so: "On older firmware the root is the whole disk, /dev/sda, and the steps that say so below are left out."

On a Standard or later computer, make the partition first. The walkthrough marks the steps for modern firmware, and you type them in order: a new table, a 512 MB partition, its type set to EFI System (type 1), and then the partition formatted as FAT. The chapter [The disks](jsc:preparing_disks) shows each step.

Both kinds take the same keys while the computer starts. [[DEL]] enters the Setup, the firmware's settings, and [[F12]] opens the Boot Menu ([](jsc:firmware)).

## If something goes wrong

> [!PROBLEM] The bootloader step refuses on a Standard or later computer.
> The start program needs the FAT partition, mounted where that step says. Make the partition first, as the walkthrough's modern steps show ([](jsc:the_walkthrough)).

> [!SEE] jsc:firmware, jsc:the_systems
