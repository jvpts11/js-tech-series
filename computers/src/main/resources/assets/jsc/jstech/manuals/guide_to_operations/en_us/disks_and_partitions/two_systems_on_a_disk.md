---
section: disks_and_partitions
order: 31
icon: jsc:disk_vaultis_trench_20m
---
# Two systems on one disk

Each installer asks you where the system goes, the way its real one did, and can put it beside another system on the same disk.

Table: Where each installer puts its system

| Installer | What it asks |
| --- | --- |
| MC-DOS, UNIX System V, Frames 95 | Nothing: it takes the disk and formats it. |
| Frames XP | A list of the partitions and the free space, in text mode; then how to format the one you pick. |
| Frames 7 to 11 | A table of the partitions, with New, Delete, Format and Extend under it. |
| Ubuntu, Linux Mint | Install alongside the other system: a divider shrinks its partition, and the arrow keys move it. |
| Debian, Fedora | Each disk with its partitions, and where the system will go. |
| FreeBSD | Auto (ZFS), with a mirror on a second disk if you want one, or Auto (UFS). |

On a UEFI machine the installer makes the EFI partition first, when the disk has none. On Linux, the BSDs and UNIX it also makes a swap partition as big as the memory, when there is room for one ([](jsc:swap)).

## Choosing what starts

With two systems on a machine, something has to ask which one starts. On the Standard and later, the GRUB menu of a Linux system lists every other system, and the Frames Boot Manager of the EFI partition; the Midsoft Boot Manager lists each Frames with its partition. The firmware's boot menu, opened with F12 as the machine starts, has one line for every system on every partition.

On the Vintage, the disk starts its active partition, and nothing asks. To start the other system, make its partition the active one: option 2 of FDISK on MC-DOS, or of fdisk on UNIX.

1. Install the first system, and leave free space on the disk, or let the second installer shrink the first.
2. Start the second installer, and choose the free space, or Install alongside.
3. After the copy, restart. Pick the system in the boot menu, or set the active partition on the Vintage.

## If something goes wrong

> [!PROBLEM] The installer says there is no room.
> Free space on the disk first, by shrinking a volume with a disk tool.

> [!PROBLEM] An MBR disk refuses a new partition.
> It already holds four. Delete one, or use a GPT disk.

> [!SEE] jsc:disk_tools_of_each_system, jsc:partitions
