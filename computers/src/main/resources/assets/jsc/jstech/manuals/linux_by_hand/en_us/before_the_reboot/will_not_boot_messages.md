---
section: before_the_reboot
order: 329
---
# The refusals and their fixes

A refusal is the reboot command telling you that it did not restart the machine. Nothing is lost when it refuses: your live session stays open. Type the missing step, then run reboot again.

The lines appear in the live shell after you type reboot, each one exactly as shown in the table. The table gives what each line means and what you do about it.

Table: The lines the reboot command prints

| The line | What it means | The fix |
| --- | --- | --- |
| `reboot: you are inside the new system. exit first.` | You are still inside the new system, entered with chroot. | Type exit to leave the new system, then run reboot. |
| `The new system will not boot:` | Items of the checklist are still missing, one for each line under it. | Do the step that each line names ([](jsc:reboot_checklist)), then run reboot again. |
| `Rebooting into the new system ...` | Nothing is missing. The installation is written to the disk, and the machine restarts. | Nothing: the machine restarts. |
| `The installation could not be written to the disk (no space or no disk).` | No disk has room for the system, or there is no disk to write to. | Free space on the disk, or put a disk in the computer, then run reboot again. |

## The reboot does not check the mounts

The reboot command does not check whether the new system's disk is still mounted. Only the checklist and the disk write decide. The Gentoo and Arch guides still tell you to unmount first with umount -R, so do that before you run reboot, as the guide says.

A chroot error about a root folder that does not exist comes from the chroot command. It is not a refusal of the reboot.

## If something goes wrong

> [!PROBLEM] reboot says you are inside the new system.
> Type exit, which leaves the new system, then run reboot.

> [!PROBLEM] The disk message shows, with no space or no disk.
> Free space on the disk, or put a disk in the computer. Then run reboot again.

> [!PROBLEM] The same list shows again after you did its steps.
> Run that step again, read what it prints, then type reboot again.
