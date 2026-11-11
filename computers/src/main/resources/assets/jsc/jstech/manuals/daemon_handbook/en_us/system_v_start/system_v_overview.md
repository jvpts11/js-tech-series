---
section: system_v_start
order: 349
---
# What UNIX System V is

UNIX System V is the oldest of the UNIX systems you meet at a prompt, and the one that a computer of the first era runs. Its release is 3.2. You work in it the way you work in any UNIX system: you type a command, the system answers, and your files sit in folders.

The system was built for a small machine. It takes 10 megabytes (MB) of disk, and it keeps 2 megabytes (MB) of memory for itself while it runs, yet it runs several programs at once. It takes its programs from media and from nowhere else: there is no Mirror to install from, only media ([](jsc:programs)).

```table
{"caption": "UNIX System V at a glance", "rows": [[{"words": "Release"}, {"words": "3.2"}], [{"words": "Hardware era"}, {"words": "Vintage, the first era"}], [{"words": "Space on the disk"}, {"amount": 10, "unit": "MB"}], [{"words": "Memory kept for the system while it runs"}, {"amount": 2, "unit": "MB"}], [{"words": "Install medium"}, {"words": "Floppy disk, read by a Floppy Drive"}], [{"words": "Shell"}, {"words": "sh"}], [{"words": "Package manager"}, {"words": "None: programs come from media"}], [{"words": "Software house"}, {"words": "Bellwether Labs"}], [{"words": "Desktop"}, {"words": "CDE (Common Desktop Environment), installed as a program"}]]}
```

> [!DEFINE] Install medium
> An item that carries the files of a system or a program. It goes into a drive linked to the computer, and the system reads it from there.

Starting the system prints a few lines in order, and the last one says the system is ready.

```table
{"caption": "The lines at start", "heads": ["Line", "What it tells you"], "rows": [[{"literal": "UNIX System V Release 3.2"}, {"words": "The system's name and its release."}], [{"literal": "Copyright (c) 1984, 1986, 1987 Bellwether Labs"}, {"words": "The software house's notice."}], [{"literal": "All Rights Reserved"}, {"words": "The end of the notice."}], [{"literal": "Total real memory     = <n>"}, {"words": "The memory the computer has."}], [{"literal": "Available memory      = <n>"}, {"words": "The memory the system can use, after the kernel keeps its share."}], [{"literal": "The system is coming up.  Please wait."}, {"words": "The system is starting."}], [{"literal": "/dev/root: clean"}, {"words": "The disk the system starts from passed its check."}], [{"literal": "The system is ready."}, {"words": "The prompt is ready for your first command."}]]}
```

When you shut the system down, it prints a warning first: "THE SYSTEM IS BEING SHUT DOWN NOW ! ! !", then "Log off now or risk your files being damaged."

The last line says "The system is down.", or "The system is being restarted." when the system comes straight back up.

## Folders

Your home folder is /usr/player, and the people of the system live under /usr. The installation makes bin, etc, mnt and tmp under the root, and bin, lib and player inside usr. Inside your home it makes Desktop and Documents.

Programs go to /usr/bin. When a drive holds a medium, its files appear under /mnt with the letter of the drive, such as /mnt/d.

## Commands

Most commands of the shell work here, and [the shell](jsc:shell_prompt) explains them. Two commands exist only on this system: installpkg, which installs a program from a medium, and pace, the menu system. Both have their own entries ([](jsc:installpkg), [](jsc:pace)).

> [!DEFINE] PACE
> The menu system of UNIX System V, with its menus in frames over one another.

Table: Commands that this system lacks

| Command | It belongs to |
| --- | --- |
| `ee` | FreeBSD |
| `info` | Linux |
| `lspci, lsusb` | Linux |
| `pciconf, usbconfig` | FreeBSD |

## If something goes wrong

> [!PROBLEM] You type a command you know from another system, and the prompt does not know it.
> Check the table above: some commands belong to other systems. Use the command this system has.

> [!PROBLEM] The system starts at a prompt, and you expect a desktop.
> A desktop is a program you install. On a Legacy computer or a newer one, install CDE ([](jsc:cde_on_system_v)), then restart the computer. The desktop shows at the next boot.

> [!SEE] jsc:installing, jsc:the_systems, jsc:shell_prompt
