---
section: midsoft_installs
order: 198
icon: jsc:cd_rom
---
# Setting up Frames 95 and XP

Frames 95 and Frames XP both come on a CD, so you need a CD Drive joined to the computer ([](jsc:drives)). The disc holds the setup program, SETUP.EXE, which copies the system onto a disk. Frames 95 and XP each have their own setup pages, and the keys below matter on the Frames XP disk page.

## Frames 95

The firmware offers the disc as "Frames 95 installer", and choosing it starts the setup ([](jsc:firmware)).

1. Put the Frames 95 CD in a CD Drive that is joined to the computer.
2. Turn the computer on, and choose "Frames 95 installer" in the firmware.
3. Read the check on the first page, then press [[Enter]] to continue, or [[F3]] to exit.
4. Choose the disk for the system, then go on to the next page.
5. Type the computer's name, then go on.
6. Wait while the files are copied.
7. When the last page says "Setup is finished.", the copy is done.
8. You can then take the CD out of the drive.

Table: The pages of Frames 95

| Page | What it shows |
| --- | --- |
| Welcome | The check: Generation, Processor, Memory, the disk and the installation medium, each with its answer. |
| Disk | "Choose a Disk", with the disks of the machine. |
| Name | "Computer Name": the name is cut to 256 letters. |
| Copy | "Copying Files". |
| Done | "Setup is finished." |

## Frames XP

Frames XP comes on the same CD and starts the same way: choose "Frames XP installer" in the firmware. Its pages are different, and its disk page lists the keys you need.

Table: The keys on the Frames XP disk page

| Key | What it does |
| --- | --- |
| `ENTER` | Installs the system on the chosen disk. |
| `E` | Erases the chosen disk, after you confirm it twice. |
| `F3` | Quits the setup. |

Table: The pages of Frames XP

| Page | What it shows |
| --- | --- |
| Disk | "The following list shows the disks on this computer." |
| Name | "Computer Name". |
| Copy | "Please wait while Setup copies files to the disk." |
| Install | "Installing Frames", with the steps Installing Frames and Finalizing installation. |
| Done | "Setup is finished.", and the key ENTER restarts the computer. |

## Choosing the disk

Both setups use the same rules for the disk. The setup proposes the first disk with room for the system. A disk has room when its free space is at least the system's footprint.

If the chosen disk is too small, the setup says "No room here. Erase this disk, or choose another." Erasing is asked twice: the first answer only asks, and the second erases the disk. The question reads "Erase Disk" with the disk's number. The erase happens before the copy, and choosing another disk before the copy starts cancels it.

> [!DEFINE] Setup
> The installer program that copies a system onto a disk.

## If something goes wrong

> [!PROBLEM] The screen says: "No disk in this machine has room for it."
> Erase a disk you no longer need, or add a bigger disk ([](jsc:disks)).

> [!PROBLEM] The screen says: "No room here. Erase this disk, or choose another."
> Choose another disk from the list, or erase this one if its files are not needed.

> [!PROBLEM] The firmware does not offer the installer, and says: "No installation medium is in a drive linked to this machine."
> Put the CD in a CD Drive that is joined to the computer ([](jsc:peripheral_cables)).

> [!PROBLEM] The copy stops and says: "The medium was taken out before the system was written."
> Put the CD back in the drive and start the setup again. Leave the CD in until the copy is done.

> [!SEE] jsc:frames_setup_7_10_11, jsc:frames_95, jsc:frames_xp, jsc:installing
