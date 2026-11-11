---
section: arch_system
order: 313
icon: jsc:cd_rom
---
# Rebuilding the boot images (mkinitcpio)

Before the real system starts, the kernel needs a small temporary system that loads the drivers for the disks. That system is the initramfs. Arch builds two of them, called boot images: a normal one, and a fallback for when the normal one does not start.

> [!DEFINE] initramfs
> A small temporary system that starts before the real one, to load the drivers for the disks.

The tool that builds them is mkinitcpio. Type mkinitcpio -P to build both images again. The tool works on the new system, so you must be inside it.

1. Make sure you are inside the new system.
2. Type mkinitcpio -P and press [[Enter]].
3. Wait until the prompt comes back.

```session
{"lines": [{"prompt": "[root@archiso /]# ", "text": "mkinitcpio -P", "typed": true}, {"text": "==> Building image from preset: /etc/mkinitcpio.d/linux.preset: 'default'"}, {"text": "==> Using default configuration file: '/etc/mkinitcpio.conf'"}, {"text": "  -> -k /boot/vmlinuz-linux -g /boot/initramfs-linux.img"}, {"text": "==> Starting build: '6.11.5-arch1-1'"}], "look": "jsc:guide/terminal/zsh", "title": "Arch Linux"}
```

Each image is built in steps called hooks. There are twelve: base, udev, autodetect, microcode, modconf, kms, keyboard, keymap, consolefont, block, filesystems and fsck.

> [!DEFINE] hook
> One step of building a boot image.

The fallback image skips the step autodetect. Pacstrap has already built both images in its last steps, so this command is for when you want them built again. On a slow processor it takes longer.

## If something goes wrong

> [!PROBLEM] The screen says "mkinitcpio: command not found (it is the new system's: arch-chroot /mnt first)".
> You are on the live medium. Step in first with arch-chroot /mnt ([](jsc:arch_chroot)).
