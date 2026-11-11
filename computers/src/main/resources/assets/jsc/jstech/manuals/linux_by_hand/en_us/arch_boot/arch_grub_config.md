---
section: arch_boot
order: 315
icon: jsc:cd_rom
---
# Writing the boot list (grub-mkconfig)

The boot loader needs a list of what to start. That list is a file, and the tool grub-mkconfig writes it. It finds the kernel and the boot images in /boot, and writes one menu entry for them.

> [!DEFINE] grub.cfg
> The file the boot loader reads when it starts. It says which kernel to start, and how.

The file is always written to /boot/grub/grub.cfg, whatever you type after -o.

1. Make sure you are inside the new system.
2. Type grub-mkconfig -o /boot/grub/grub.cfg and press [[Enter]].
3. Wait for done at the end of the output.

```session
{"lines": [{"prompt": "[root@archiso /]# ", "text": "grub-mkconfig -o /boot/grub/grub.cfg", "typed": true}, {"text": "Generating grub configuration file ..."}, {"text": "Found linux image: /boot/vmlinuz-linux"}, {"text": "Found initrd image: /boot/initramfs-linux.img"}, {"text": "Warning: os-prober will not be executed to detect other bootable"}, {"text": "partitions. Systems on them will not be added to the GRUB boot"}, {"text": "configuration. Check GRUB_DISABLE_OS_PROBER documentation entry."}, {"text": "done"}], "look": "jsc:guide/terminal/zsh", "title": "Arch Linux"}
```

The first line of the output says the configuration file is being generated. A warning follows, and it is expected. os-prober is a tool that looks for other systems on other disks, and here it does not run, so no other system gets an entry. On UEFI, the output also says it adds a boot menu entry for the firmware settings.

The file has one menu entry, named Arch Linux. It loads the kernel and the boot images, and it tells the kernel which partition is the root, by its UUID.

## If something goes wrong

> [!PROBLEM] The screen says "/usr/bin/grub-mkconfig: /boot/grub: No such file or directory".
> The boot loader is not installed yet, so its folder does not exist. Install it first ([](jsc:arch_bootloader)).

> [!PROBLEM] The screen says "Usage: grub-mkconfig -o /boot/grub/grub.cfg".
> The command has no -o. Type grub-mkconfig -o /boot/grub/grub.cfg.

> [!PROBLEM] The screen says "grub-mkconfig: command not found (it is the new system's: step into it first)".
> You are on the live medium. Step in first with arch-chroot /mnt ([](jsc:arch_chroot)).

> [!SEE] jsc:arch_reboot
