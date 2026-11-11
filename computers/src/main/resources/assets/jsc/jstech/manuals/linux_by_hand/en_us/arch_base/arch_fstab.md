---
section: arch_base
order: 310
icon: jsc:cd_rom
---
# Writing the file system table (genfstab)

A system must know which disk to mount, and where, every time it starts. That list lives in one file, /etc/fstab, the file system table. The tool genfstab writes that file for you, from the disks you mounted.

Most lines name a partition by its universally unique identifier (UUID), which the file system carries on its disk. The UUID stays the same when the disk gets another letter, so a renamed disk still boots. Here the same disk always gives the same UUID.

1. Make sure the base system is installed.
2. Type genfstab -U /mnt and press [[Enter]]. The table is printed, and nothing is written.
3. Type genfstab -U /mnt >> /mnt/etc/fstab and press [[Enter]]. The table is written into the file.

Print the table first if you want to check it. The printed table is the same text the redirection writes, so what you see is what the file gets.

Table: The lines of the table

| Line | What it says |
| --- | --- |
| `# /dev/sda2` | A comment: the partition the line is about. |
| `UUID=` | The partition's UUID, then the folder it mounts on. |
| `/` | The folder the partition mounts on: the top of the system. |
| `ext4` | The file system type of the partition. |
| `rw,relatime` | The options: the disk is read and written, and access times are recorded in a lighter way. |

If you mounted the boot partition, a second line follows after a blank line. It names that partition by its serial number, the identifier of the File Allocation Table (FAT) file system, and the folder it is mounted on, such as /boot.

> [!DEFINE] fstab
> The file in /etc that lists which partitions to mount where, and how, when the system starts.

> [!DEFINE] UUID
> A long identifier that a file system carries on its disk, so the system finds the partition whatever its device name is.

## If something goes wrong

> [!PROBLEM] The screen says "usage: genfstab [options] root".
> The command does not name /mnt. Type genfstab -U /mnt >> /mnt/etc/fstab.

> [!PROBLEM] The screen says "==> ERROR: /mnt/etc does not exist".
> Pacstrap has not run yet. Install the base system first ([](jsc:arch_base_system)).

> [!SEE] jsc:arch_chroot
