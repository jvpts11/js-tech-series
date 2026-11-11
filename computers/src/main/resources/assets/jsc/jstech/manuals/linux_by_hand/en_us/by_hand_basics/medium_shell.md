---
section: by_hand_basics
order: 303
---
# The medium's commands and Ctrl+C

The shell of the medium answers a fixed list of commands. Most are the real tools of the installation, with their real names. A few, such as help, exit and reboot, are the medium's own.

A word the shell does not know gets the refusal of the system the shell belongs to: bash on Gentoo, zsh on Arch Linux. Each system has its own commands, so a command of the other system is refused.

Table: Commands on both systems

| Group | Commands |
| --- | --- |
| Files and folders | `ls, cat, less, more, cd, echo, nano, mkdir` |
| Disks | `lsblk, blkid, fdisk, mkfs, mkfs.ext4, mkfs.fat, mkfs.vfat, mount, umount` |
| Entering the new system | `chroot` |
| Settings | `source, export, env-update, hwclock, hostname` |
| Start program | `grub-install, grub-mkconfig` |
| The medium's own | `help, exit, reboot` |

Table: Commands of one system

| System | Its own commands |
| --- | --- |
| Arch Linux | `pacstrap, genfstab, pacman, mkinitcpio, arch-chroot` |
| Gentoo | `wget, tar, emerge-webrsync, emerge, eselect, genkernel, make` |

The prompt tells you where you are. Outside the new system it shows the medium's name, and inside the new system it changes as the table shows. The ~ is the root's own folder, and any other folder shows its own path.

Table: The prompt

| System | Outside the new system | Inside the new system |
| --- | --- | --- |
| Arch Linux | `root@archiso ~ #` | `[root@archiso ~]#` |
| Gentoo | `livecd ~ #` | `(chroot) livecd ~ #` |

## Entering and leaving the new system

You step into the new system with chroot on either system, or with arch-chroot on Arch Linux only. Once inside, the commands and the files you use are the new system's. Type exit to step back out. Outside the new system, exit prints logout.

## The partition editor

fdisk opens a partition editor that takes over the terminal. Each command is a letter, with an optional value after it. You type it on its own line and press [[Enter]]. The chapter [The disks](jsc:preparing_disks) takes you through a whole session in this editor.

Table: Commands of the partition editor

| Command | What it does |
| --- | --- |
| `m` | Shows the editor's help, the same as typing help. |
| `g` | Makes a new, empty partition table (GUID Partition Table, GPT). |
| `p` | Prints the table as it is now. |
| `n` | Makes a new partition, and n 512M gives it 512 MB on the same line. |
| `t` | Sets the type of a partition: t 1 marks it for UEFI. |
| `d` | Deletes a partition. |
| `w` | Writes the table to the disk, the only command that changes the disk. |
| `q` | Quits without writing, and throws away everything typed since the editor opened. |

## Ctrl+C

Press [[Ctrl]]+[[C]] to stop the tool that is running. The prompt comes back, and the part of the work the tool had not finished stays not done.

With no tool running, [[Ctrl]]+[[C]] does nothing. The tools that take time are pacstrap, pacman, emerge, emerge-webrsync, genkernel, make, wget, tar, grub-install, grub-mkconfig, fdisk and the mkfs tools. Their output appears as it runs.

> [!WARNING] In fdisk, [[Ctrl]]+[[C]] throws away everything typed since the editor opened, the same as q. Only w writes the disk.

## If something goes wrong

> [!PROBLEM] bash: pacstrap: command not found (Gentoo), or zsh: command not found: wget (Arch Linux).
> The word is not a command of your system. Check the list of commands above: pacstrap belongs to Arch Linux, and wget to Gentoo.

> [!PROBLEM] emerge: command not found, and the message says the package manager is the new system's.
> Step into the new system first, with chroot /mnt/gentoo /bin/bash, then type emerge there ([the chapter Gentoo](jsc:gentoo)).

> [!PROBLEM] mkinitcpio: command not found, and the message says it is the new system's.
> Step into the new system first, with arch-chroot /mnt, then type mkinitcpio ([the chapter Arch](jsc:arch)).

> [!PROBLEM] grub-install: command not found, and the message says it is the new system's.
> Step into the new system first, then type the command again.

> [!PROBLEM] chroot: cannot change root directory to '/mnt': No such file or directory
> The folder is not the root of the new system. Mount the root partition at /mnt on Arch Linux, or at /mnt/gentoo on Gentoo, first ([the chapter The disks](jsc:preparing_disks)).

> [!PROBLEM] chroot: failed to run command '/bin/bash': No such file or directory
> The base system is not installed yet. Install it first, with pacstrap on Arch Linux or with wget and tar on Gentoo.

> [!PROBLEM] fdisk: cannot open /dev/sdc: No such file or directory
> There is no such disk. Type lsblk to list the disks the shell can see.

> [!PROBLEM] nano: /mnt/gentoo is a directory
> nano opens files, not folders. Name a file after nano.

> [!SEE] jsc:installing, jsc:firmware_for_install
