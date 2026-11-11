---
section: starting_a_computer
order: 4
icon: jsc:motherboard_atx_standard_lga1150
---
# Firmware: the BIOS and the UEFI

The firmware lives on the motherboard, not on any disk, so it is there before any system. It is the first program that runs when the computer starts: it checks the hardware ([](jsc:turning_on)), finds the disks and drives, and hands the computer to a system. If no system is found, it waits on the screen for you.

Its kind depends on the era of the motherboard. Older boards have a BIOS, and newer boards have a UEFI. Both are firmware, and each era has its own look:

Table: The firmware of each era

| Era | Firmware on the screen |
| --- | --- |
| Vintage | Text BIOS, plain text on the screen. |
| Legacy and Transition | Blue BIOS. |
| Standard and Advanced | UEFI. |

A board can also come with no firmware on it: its tooltip says so. A computer built on it powers up and turns its fans, and nothing more: no self-test, no picture, and the monitor says the motherboard has no firmware. A Preinstaller writes the firmware of the board's era onto the board, or onto the computer it is in ([](jsc:computer_making_machines)).

The boot order is the list of disks in the order the firmware tries them. You change the order on the Boot Order page of the Setup ([](jsc:setup_utility)).

> [!DEFINE] Boot order
> The order the firmware tries the disks in when the computer starts. The first disk with a system on it starts.

The Boot Menu ([](jsc:boot_menu)) is the same choice for one start only. A disk you pick there is not saved, so the next start follows the boot order again.

A disk with two systems on it can show a menu before it starts. See [Two systems on one computer](jsc:two_systems).

The Setup also has a Hardware page, which lists what the firmware found in the machine. On a server with a storage controller, it has a Storage page for the array of its drives ([](jsc:raid_arrays)).

> [!DEFINE] BIOS
> Basic Input/Output System: the older kind of firmware. The Vintage, Legacy and Transition boards have a BIOS.

> [!DEFINE] UEFI
> Unified Extensible Firmware Interface: the newer kind of firmware. The Standard and Advanced boards have a UEFI.

## If something goes wrong

> [!PROBLEM] The screen reports that nothing can start, in words that depend on the era.
> No disk has a system yet, and no install medium is in a drive. Put one in, and start from it ([](jsc:installing)). The exact words depend on the era ([](jsc:no_bootable_device)).

> [!PROBLEM] A disk is not in the list.
> The disk is not connected to the computer, or the medium is in the wrong kind of drive. A floppy goes in a Floppy Drive and a CD in a CD Drive ([](jsc:drives)).

> [!PROBLEM] The computer starts from the wrong disk.
> Change the boot order on the Boot Order page of the Setup, or pick the disk in the Boot Menu ([](jsc:setup_utility)).
