---
section: before_the_reboot
order: 330
---
# Which typed files reach the installed system

Much of a hand install happens in the live shell, in files you type or edit: the table, the name, the kernel, the bootloader's list. The installed system does not get all of them. The reboot checklist records the steps you ran, and on Gentoo it also reads the table you wrote.

While you work, the files you type go under the new system's root folder in the live session: /mnt on Arch, and /mnt/gentoo on Gentoo.

## What reaches the disk

Table: What the installation copies to the disk

| File | Copied to the disk? |
| --- | --- |
| The machine's name | Yes: the name you chose. |
| The packages you asked for | Yes. |
| `/etc/fstab` | Yes: the table of what to mount. |
| `/etc/portage/make.conf` | Yes: the build options (Gentoo). |
| The kernel file you installed (/boot/vmlinuz-6.11.5-gentoo-x86_64 on Gentoo, /boot/vmlinuz-linux on Arch) | No: it is not copied. |
| `/boot/grub/grub.cfg` | No: the bootloader's list is not copied. |

Then the installation writes its own boot files onto the disk: a folder named boot, and the kernel file boot/vmlinuz inside it. It writes them whatever you typed, so the machine can start even though the kernel file you installed is not copied.
