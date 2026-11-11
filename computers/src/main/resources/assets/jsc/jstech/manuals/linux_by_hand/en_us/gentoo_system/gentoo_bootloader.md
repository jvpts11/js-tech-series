---
section: gentoo_system
order: 325
---
# GRUB on Gentoo

The bootloader starts the kernel you built, and it shows the menu of the systems on the disk. On Gentoo the bootloader is GRUB ([](jsc:arch_bootloader)). GRUB is a package like any other, so you compile it from source too.

The firmware decides where GRUB goes. UEFI (Unified Extensible Firmware Interface) reads it from the EFI system partition. BIOS (basic input/output system) finds it on the disk itself.

1. Type emerge --ask sys-boot/grub and press [[Enter]]. Answer the question with Enter.
2. Type the install command for your firmware, from the table below, and press [[Enter]].
3. Type grub-mkconfig -o /boot/grub/grub.cfg and press [[Enter]].

Table: The install command for each firmware

| Firmware | Command |
| --- | --- |
| UEFI | `grub-install --efi-directory=/efi` |
| BIOS (older firmware) | `grub-install /dev/sda` |

The --efi-directory=/efi part must name the place where the EFI system partition is mounted in the new system. The mount step put it at /mnt/gentoo/efi, which is /efi inside the new system.

The menu that grub-mkconfig writes has one entry, named Gentoo Linux. The entry starts the kernel you built, and it gives the kernel your root partition by its UUID.

## If something goes wrong

> [!PROBLEM] bash says "grub-install: command not found".
> GRUB is not compiled yet. Type emerge --ask sys-boot/grub first.

> [!PROBLEM] grub-install says "command not found". The program belongs to the new system and is not built there yet.
> Step into the new system first: chroot /mnt/gentoo /bin/bash, then run the command there.

> [!PROBLEM] grub-install says "warning: no kernel image found in /boot."
> Build the kernel first: genkernel all, or make in /usr/src/linux ([](jsc:kernel_gentoo)).

> [!PROBLEM] grub-install says "error: /boot/efi doesn't look like an EFI partition."
> The EFI system partition is not mounted where grub-install looks. Mount it, then name its place with --efi-directory=/efi, as the walkthrough does.

> [!PROBLEM] grub-install says "error: install device isn't specified."
> BIOS firmware needs the disk named. Type grub-install /dev/sda.

> [!PROBLEM] grub-install says "error: cannot find a device for /dev/<dev>."
> GRUB goes on the disk, not in a partition. Type grub-install /dev/sda.

> [!PROBLEM] grub-mkconfig says "/boot/grub: No such file or directory".
> GRUB is not installed yet. Run grub-install first.

> [!SEE] jsc:arch_bootloader, jsc:arch_grub_config, jsc:gentoo_reboot
