---
section: freebsd_system
order: 334
---
# The loader and the boot lines

After the firmware, the computer runs the loader. The loader is the program at the start of the disk that loads the kernel, which is the core of the system. It is the boot manager of FreeBSD, and it can show a short menu for five seconds before it starts the system.

> [!DEFINE] Loader
> The program at the start of the disk that loads the kernel.

> [!DEFINE] Kernel
> The core of the system, and the first thing the boot lines name.

> [!DEFINE] Boot lines
> The text the system prints on its way up, when it starts, and on its way down, when it stops.

The loader does not choose between systems. It starts FreeBSD from the system disk and lists no other system. If you want another system on the same computer, use the firmware's boot order or its Boot Menu ([](jsc:firmware), [](jsc:boot_menu)).

## The loader's menu

Table: The entries of the loader's menu

| Entry | What it does |
| --- | --- |
| Boot | Starts the running system. It is the default: Enter starts it. |
| Reboot | Starts the machine over. |
| Firmware settings | Opens the firmware's settings, shown only on Standard and later hardware, which uses UEFI (the Unified Extensible Firmware Interface). |

The menu counts down from 5 seconds. Its top line says "Welcome to FreeBSD", and the line under it says "Autoboot in 5 seconds. [Space] to pause".

Any key stops the count, although the line names the space bar. Once the count stops, press [[Enter]] to start the default entry, or a number from 1 to 9 to start that entry at once. Keys beyond the entries do nothing.

The server setting boot.show_boot_menu turns the menu on or off. It is on by default. When it is off, the computer starts the system at once.

## The boot lines on the way up

On its way up, FreeBSD prints a fixed set of lines. Each line appears only when its condition holds, as the last column says.

```table
{"caption": "The boot lines on the way up", "heads": ["Line", "When it appears"], "rows": [[{"literal": "Loading kernel..., then Booting..."}, {"words": "At the start."}], [{"literal": "FreeBSD 14.1-RELEASE GENERIC <architecture>"}, {"words": "Always."}], [{"literal": "CPU: <processor name>"}, {"words": "Always: the processor of the computer."}], [{"literal": "real memory  = <n> MB"}, {"words": "Always: the memory the computer has."}], [{"literal": "ada0: <<disk name>>"}, {"words": "One line for each disk, then ada1 and so on."}], [{"literal": "Trying to mount root from ufs:/dev/ada0p2 [rw]..."}, {"words": "Always: the system's own disk."}], [{"literal": "Setting hostname: <hostname>."}, {"words": "Always."}], [{"literal": "Starting Network: lo0 em0."}, {"words": "Only when a data cable reaches the computer."}], [{"literal": "pkg: repository Mirror found on <mirror host>."}, {"words": "Only when a Mirror answers."}], [{"literal": "Starting devd."}, {"words": "Always."}], [{"literal": "Starting syslogd."}, {"words": "Always."}], [{"literal": "Starting cron."}, {"words": "Only when cron is on."}], [{"literal": "Starting sshd."}, {"words": "Only when remote is allowed."}], [{"literal": "Starting <desktop> display manager."}, {"words": "Only when a desktop is installed."}]]}
```

## The boot lines on the way down

When you stop or restart the computer, FreeBSD prints its way down. It stops each service that is running, and it syncs the disks. The last line says "Rebooting..." when the computer restarts, and "The operating system has halted." when it stops.

## When the system's file is missing

The loader starts the file boot/kernel, in the folder boot on the system disk. Without that file, FreeBSD cannot start. To repair it, install over it from a medium.

## If something goes wrong

> [!PROBLEM] The computer waits at the loader's menu, and nothing starts.
> Press Enter to start Boot, or wait 5 seconds for it to start on its own.

> [!PROBLEM] The computer starts a system other than FreeBSD.
> The loader starts only FreeBSD. Change the firmware's boot order, or use its Boot Menu ([](jsc:firmware)).

> [!PROBLEM] The loader's menu does not appear.
> The server setting boot.show_boot_menu is off. Turn it on to show the menu again.

> [!PROBLEM] The system's file boot/kernel is missing from the system disk.
> Boot an installation medium and install over it to repair.

> [!SEE] jsc:firmware, jsc:boot_menu, jsc:bsdinstall
