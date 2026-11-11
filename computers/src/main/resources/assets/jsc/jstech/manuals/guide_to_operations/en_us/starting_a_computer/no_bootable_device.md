---
section: starting_a_computer
order: 9
icon: jsc:floppy_disk
---
# When nothing will boot

Sometimes the firmware finds nothing it can start. Then it says so on the screen and waits for you. The words depend on the era of the motherboard.

Table: What the screen says

| Era | What the screen says |
| --- | --- |
| Vintage | "Non-system disk or disk error", then "Replace and press any key when ready". |
| Legacy and Transition | "DISK BOOT FAILURE, INSERT SYSTEM DISK AND PRESS ENTER". |
| Standard and Advanced | "No bootable device found", then "Press any key to enter Setup". |

When a disk holds a system whose boot loader was deleted, that system's own complaint replaces the firmware's text. The screen then adds "Put in an installation medium and install over it to repair."

The screen lists each disk and drive it knows. When there are more than it can show, the last line says how many more there are. An empty list says "No disk and no drive is attached to this computer."

The hint reads "Insert installation media and press Enter, or DEL for Setup". Press any other key, and the screen says Entering Setup and opens the Setup ([](jsc:setup_utility)).

1. Put an install medium into a drive that is joined to the computer.
2. Press Enter on this screen to start from it, or press DEL to go to the Setup.
3. Follow the installer ([](jsc:installing)).

Table: Which drive reads which medium

| Medium | Drive that reads it |
| --- | --- |
| Floppy Disk | Floppy Drive. |
| CD-ROM | CD Drive, DVD Drive or Blu-ray Drive. |
| DVD-ROM | DVD Drive or Blu-ray Drive. |
| USB Flash Drive | Dock Station, or the USB port on the front of a case from the Transition on. |

## If something goes wrong

> [!PROBLEM] The screen says that no bootable device was found.
> No disk has a system yet, and no install medium is in a drive. Put one in ([](jsc:installing)).

> [!PROBLEM] A medium is in a drive, but the screen does not list it.
> The medium is in the wrong kind of drive, or the drive is not joined to the computer. See the table above ([](jsc:drives)).

> [!PROBLEM] The screen says "No disk and no drive is attached to this computer."
> Join a drive, or a disk, to the computer ([](jsc:drives)).

> [!SEE] jsc:installing, jsc:drives, jsc:boot_menu
