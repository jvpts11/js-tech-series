---
section: gentoo_base
order: 321
---
# Mounts and chroot

The new system lives on your disk, not on the medium. To reach it, you mount the disk's partitions into folders of the live medium ([](jsc:mounting)). The root partition goes to /mnt/gentoo.

Then you step into the new system with chroot ([](jsc:arch_chroot)). Chroot makes /mnt/gentoo look like the root folder /, so every command you type runs in the new system.

1. Type mount /dev/sda2 /mnt/gentoo and press [[Enter]].
2. For UEFI (Unified Extensible Firmware Interface) only, type mount /dev/sda1 /mnt/gentoo/efi and press [[Enter]]. That partition is the EFI system partition (ESP).
3. Type mount --types proc /proc /mnt/gentoo/proc and press [[Enter]].
4. Bind the other three folders the same way: type mount --rbind /sys /mnt/gentoo/sys, then do the same for /dev and /run.
5. Type chroot /mnt/gentoo /bin/bash and press [[Enter]].
6. Type source /etc/profile and press [[Enter]]. The command prints nothing.

The folders /proc, /sys, /dev and /run are not files on your disk. The kernel fills them with live information about the machine, and the new system needs them to reach its devices. A bind shows one folder at a second place, so the new system sees them too.

Once you are inside, the prompt starts with (chroot), and the commands work on the new system. Type exit to leave it, and the live medium's own shell comes back.

## If something goes wrong

> [!PROBLEM] mount says "wrong fs type, bad option, bad superblock" for the root.
> The root partition is not formatted as ext4 yet. Format it first: mkfs.ext4 /dev/sda2 ([](jsc:filesystems_and_mkfs)).

> [!PROBLEM] mount says "mount point does not exist" for /mnt/gentoo/efi.
> Mount the root partition first. The EFI system partition goes inside it.

> [!PROBLEM] mount says "wrong fs type" for the EFI partition.
> Format that partition first: mkfs.fat -F 32 /dev/sda1.

> [!PROBLEM] mount says "mount point does not exist" when you bind a folder.
> Mount the root partition first, then bind the folders.

> [!PROBLEM] chroot says "cannot change root directory" and names the path.
> Type the path /mnt/gentoo exactly, with the disk mounted there.

> [!PROBLEM] chroot says "failed to run command '/bin/bash': No such file or directory".
> The stage 3 is not unpacked yet. Unpack it into /mnt/gentoo first ([](jsc:stage_three)).

> [!PROBLEM] bash says "arch-chroot: command not found".
> Gentoo has no arch-chroot. Step in with chroot /mnt/gentoo /bin/bash.

> [!SEE] jsc:arch_chroot, jsc:mounting, jsc:stage_three, jsc:gentoo_reboot
