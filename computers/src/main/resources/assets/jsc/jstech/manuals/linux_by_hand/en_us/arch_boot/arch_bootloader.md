---
section: arch_boot
order: 314
icon: jsc:cd_rom
---
# Installing the boot loader

When the firmware starts a disk, it starts the first program on it: the boot loader. The boot loader then starts the kernel with the boot images. Arch uses GRUB for that job. GRUB shows a menu, or starts its only entry, and loads the kernel.

> [!DEFINE] firmware
> The program on the motherboard that starts the computer, before any system runs.

> [!DEFINE] boot loader
> The program the firmware starts first, which then starts the kernel.

> [!DEFINE] GRUB
> The boot loader that Arch uses.

The boot loader comes as two packages, and you install both with pacman. The efibootmgr package is the tool that tells the firmware where the boot loader is. The grub-install command refuses until the grub package is installed.

Table: The packages for the boot loader

| Package | Size |
| --- | --- |
| `grub` | 7.8 MiB |
| `efibootmgr` | 0.1 MiB |

1. Make sure you are inside the new system.
2. Type pacman -S grub efibootmgr and press [[Enter]].
3. Press [[Enter]] or type Y at the question, and wait until the install finishes.
4. Type the grub-install command for your firmware, from the table below, and press [[Enter]].

```session
{"lines": [{"prompt": "[root@archiso /]# ", "text": "pacman -S grub efibootmgr", "typed": true}, {"text": ":: Proceed with installation? [Y/n]"}], "look": "jsc:guide/terminal/zsh", "title": "Arch Linux"}
```

There are two kinds of firmware: the older Basic Input/Output System (BIOS) and the newer Unified Extensible Firmware Interface (UEFI). The table shows the install for each.

Table: The install for each firmware

| Firmware | Command | Platform it reports |
| --- | --- | --- |
| UEFI, in the Standard era and later | `grub-install --efi-directory=/boot` | `x86_64-efi` |
| BIOS, in the Legacy and Transition eras | `grub-install /dev/sda` | `i386-pc` |

Which row is yours depends on your computer's firmware ([](jsc:firmware_for_install)). On older firmware, the boot loader goes on the whole disk, not in a partition. On UEFI, mount the EFI system partition at /mnt/boot before you step in ([](jsc:mounting)), so that /boot inside the new system is that partition.

On UEFI the screen says "Installing for x86_64-efi platform.", and on BIOS it says "Installing for i386-pc platform.". Both end with "Installation finished. No error reported." The install does not write the boot list. That is the next step ([](jsc:arch_grub_config)).

## If something goes wrong

> [!PROBLEM] The screen says "zsh: command not found: grub-install", with a line that says the boot loader is a package.
> The GRUB package is not installed yet. Install it first: pacman -S grub efibootmgr.

> [!PROBLEM] The screen says "grub-install: command not found (it is the new system's: step into it first)".
> You are on the live medium. Step in first with arch-chroot /mnt ([](jsc:arch_chroot)).

> [!PROBLEM] The screen says "grub-install: error: /boot/efi doesn't look like an EFI partition."
> The EFI system partition is not mounted where the command looks. Mount it at /mnt/boot before you step in ([](jsc:mounting)), then run grub-install --efi-directory=/boot.

> [!PROBLEM] The screen says "grub-install: error: install device isn't specified."
> On older firmware, name the disk: grub-install /dev/sda.

> [!PROBLEM] The screen says "grub-install: error: cannot find a device for" and the name of a partition, such as /dev/sda2.
> The boot loader goes on the disk, not in a partition. Name the disk: grub-install /dev/sda.

> [!SEE] jsc:arch_grub_config
