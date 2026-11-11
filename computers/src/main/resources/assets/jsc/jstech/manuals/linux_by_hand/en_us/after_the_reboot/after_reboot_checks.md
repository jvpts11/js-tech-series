---
section: after_the_reboot
order: 331
---
# What the machine checks when it starts

When the machine starts, it runs the power-on self-test (POST) first. At the end of the test, the machine checks the system disk once, and then it decides what to do next.

The check looks for two things on the disk: a folder named boot, and the kernel file boot/vmlinuz inside it. The kernel file is the one file that starts a Linux system. If both are there, the machine starts the system.

Table: How the machine decides

| What the check finds on the disk | What you see | What happens |
| --- | --- | --- |
| The folder boot and the kernel file are both there. | The start goes on. | The machine starts the system. With more than one system on the disk, it may show the Boot Menu first. |
| There is no folder named boot, or no system on the disk at all, and no installation medium is in a drive. | The firmware's no-device lines, below. | The machine stops at the self-test and waits. |
| The folder boot is there, but the kernel file is gone, and no installation medium is in a drive. | `kernel panic - not syncing: no init found` | The machine stops at the self-test and waits, with the repair line below. |

## The firmware's lines when nothing starts

The lines depend on the look of the machine's firmware, which follows its era. Linux systems start on Legacy machines and later.

Table: The firmware's look

| Firmware look | What the screen shows |
| --- | --- |
| Vintage | "Non-system disk or disk error", then "Replace and press any key when ready" |
| Legacy and Transition | `DISK BOOT FAILURE, INSERT SYSTEM DISK AND PRESS ENTER` |
| Standard, Advanced and later | "No bootable device found", then "Press any key to enter Setup" |

When the machine stops with a complaint, the big header on the screen shows the complaint. Without a complaint, the header reads "No bootable device". With a complaint, the screen also shows the line "Put in an installation medium and install over it to repair."

## A machine that stops

A stopped machine keeps its message on the screen, and a monitor you open later still shows it. The machine checks the disk again once a second. It starts again by itself when the disk is whole, or when an installation medium is in a drive.

To repair a wrecked system, put an installation medium in a drive and install over the system ([](jsc:installing)). The install writes the folder boot and the kernel file back onto the disk, so the machine can start again.

If you take the installation medium out of the drive that the machine started from, the machine restarts and finds nothing to start from.

> [!DEFINE] Self-test (POST)
> The start-up test that the firmware runs first each time the computer starts, before any system starts.

A computer can also refuse to start for lack of power, which is a different refusal ([](jsc:power_supplies)).

## If something goes wrong

> [!PROBLEM] The screen says "kernel panic - not syncing: no init found" and waits.
> The kernel file boot/vmlinuz is gone from the disk. Install over the system from an installation medium ([](jsc:installing)).

> [!PROBLEM] The screen shows the firmware's no-device lines and waits.
> The disk has no system, or its folder boot is gone. Install a system on the disk from an installation medium ([](jsc:installing)).

> [!SEE] jsc:firmware, jsc:installing, jsc:two_systems, jsc:disks
