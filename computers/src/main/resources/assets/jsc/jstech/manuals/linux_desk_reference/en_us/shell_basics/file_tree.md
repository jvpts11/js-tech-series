---
section: shell_basics
order: 249
---
# The tree of folders and paths

Unix shows all of a computer's storage as one tree of folders. The tree starts at the top folder, written /, and every file sits somewhere inside it.

Under the hood the storage keeps the DOS (Disk Operating System) model of drive letters, and the shell translates it. The system disk is C: and it is the top folder. Another disk appears as a folder inside /media on Linux and FreeBSD, or inside /mnt on UNIX System V.

Table: Folders you use

| Folder | Linux and FreeBSD | UNIX System V |
| --- | --- | --- |
| Your home folder | `/home/player` | `/usr/player` |
| Your Desktop, whose files the desktop shows as icons | `/home/player/Desktop` | `/usr/player/Desktop` |
| Your Documents | `/home/player/Documents` | `/usr/player/Documents` |
| The other drives, one folder each | `/media` | `/mnt` |

You name a folder or file with a path. The first character tells the shell where to start.

Table: How a path starts

| What you type | What it means |
| --- | --- |
| `/x/y` | An absolute path: it starts at the top folder and names the whole address. |
| `x/y` | A relative path: it starts at the folder you are in. |
| `~` | Your home folder. The form ~/x is a folder inside it. |
| `..` | The folder above the one you are in. |
| `/media/d/x` | The file x on drive D:, on Linux and FreeBSD. On UNIX System V the same file is /mnt/d/x. |

Commands that name a disk use its device name. The system disk is sda1, drive D: is sdb, drive E: is sdc, and each drive after that goes on by one letter.

> [!DEFINE] Path
> The address of a file or folder: its folder names, with a slash between them.

## If something goes wrong

> [!PROBLEM] I cannot find my file.
> Run pwd to see the folder you are in. A path that starts with a slash begins at the top folder, not at your folder.

> [!PROBLEM] The df command shows the device with "(no medium)" after it.
> Put a disk in that drive, then run df again ([](jsc:file_commands)).

> [!SEE] jsc:shell_prompt, jsc:file_commands
