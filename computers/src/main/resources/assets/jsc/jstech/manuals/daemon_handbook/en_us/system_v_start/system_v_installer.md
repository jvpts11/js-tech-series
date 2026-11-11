---
section: system_v_start
order: 350
---
# The installer

You install UNIX System V from its floppy, with the System V installer. The installer asks two things, the disk and the name of the computer, then copies the system onto the disk. It runs in a window with one banner in reverse video the whole way through.

You need a computer of the Vintage era or newer, with a Floppy Drive linked to it, and a disk with at least 10 MB of free space. When you finish, the disk holds UNIX System V, and the computer starts it after the reboot.

1. Put the System V floppy in a Floppy Drive linked to the computer, and start the computer from it ([](jsc:installing)).
2. On the settings page, press [[Up]] or [[Down]] to choose the disk.
3. Type the name of the computer in the Computer name box.
4. Press [[Enter]] to go on. If the disk already holds a system, press [[1]] or [[2]] first.
5. Wait for the copy. Closing the monitor does not stop it.
6. On the done page, remove the floppy and press [[Enter]] to reboot.

Table: The three pages

| Page | What it shows |
| --- | --- |
| Settings | The question of the disk, the disk list and the Computer name box. |
| Copy | Four steps, each marked copying, waiting or with a green check: Base system, Utilities, On-line help and Manual pages. |
| Done | The name, the space used and, when you installed beside a system, the boot order. |

The settings page asks "Choose the disk to install the UNIX System V on:" and shows each disk on a line. A line starts with the slot number and the label of the disk, and then gives its size, its free space and what it holds, or "Nothing".

> [!DEFINE] Disk slot
> The place in the computer where a disk sits. The installer shows the place as a number.

A disk that already holds a system asks you first. The question reads "<disk label> holds <what it holds>. What should happen to it?"

Table: Your choice on a disk that holds a system

| Choice | What happens |
| --- | --- |
| `Erase <disk> and install the UNIX System V` | The disk is wiped, and the new system goes on it alone. |
| `Install the UNIX System V beside <what it holds>` | The new system goes on the same disk, next to the old one. This choice is dimmed when the disk has no room for it. |

While the copy runs, the copy page names the disk you chose. Open the monitor again to see its progress.

The done page says "The UNIX System V is installed on <disk>." and then gives the name of the computer and the space used on the disk.

## If something goes wrong

> [!PROBLEM] The settings page says "No room here. Erase this disk, or choose another."
> The disk has too little free space for the system. Choose another disk from the list, with at least 10 MB free.

> [!PROBLEM] The settings page says "No disk in this machine has room for it."
> Put in a disk with at least 10 MB free, then choose it on the settings page.

> [!SEE] jsc:installpkg, jsc:firmware, jsc:two_systems
