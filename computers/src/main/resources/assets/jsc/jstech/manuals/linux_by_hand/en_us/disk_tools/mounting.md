---
section: disk_tools
order: 307
---
# Mounting the disks

You cannot reach a file system's files until it is mounted: attached to a folder, so that you can reach its files through that folder. The new system is built inside /mnt on Arch, or inside /mnt/gentoo on Gentoo, and the disks are mounted there.

Mount the root first. A mount that works prints nothing at all, which is how the tool says yes. Its form is mount, the partition, then the folder: the /dev/ in front of the name is optional.

Table: The places a mount can go

| Partition | Mount point, on Arch | Mount point, on Gentoo |
| --- | --- | --- |
| The root | `/mnt` | `/mnt/gentoo` |
| The EFI system partition | `/mnt/boot` | `/mnt/gentoo/efi` |

The tool accepts the root folder, and the boot or efi folder under the root, written exactly as shown, with no slash at the end. The EFI system partition has to be mounted after the root.

## The steps on Arch

1. Type mount /dev/sda2 /mnt and press [[Enter]], to mount the root.
2. Type mount --mkdir /dev/sda1 /mnt/boot and press [[Enter]], to mount the EFI system partition.

## The steps on Gentoo

1. Type mount /dev/sda2 /mnt/gentoo and press [[Enter]], to mount the root.
2. Type mount /dev/sda1 /mnt/gentoo/efi and press [[Enter]], to mount the EFI system partition.

The tool makes the folder of the EFI system partition anyway, so you can leave out --mkdir. It ignores that flag if you type it, and it ignores the option word after -o, such as noatime.

## Binding the medium's own folders

Gentoo's checklist asks for four folders of the live medium to be bound into the new system before you step into it: /proc, /sys, /dev and /run. The new system needs them while it is set up. You bind them with the type proc, or with the flag --rbind or --bind.

A bind goes after the root is mounted, and it names the folder of the medium first, then the place in the new system. For example, mount --types proc /proc /mnt/gentoo/proc binds /proc. Each bind is silent when it works.

> [!DEFINE] Bind mount
> A folder of the running medium that appears again inside the new system, with the same contents.

## If something goes wrong

> [!PROBLEM] mount: bad usage
> You gave fewer than two words. Type mount /dev/sda2 /mnt, with the partition and the folder.

> [!PROBLEM] mount: /mnt/boot: mount point does not exist.
> The root is not mounted yet, and the EFI system partition comes after it. Mount the root first with mount /dev/sda2 /mnt.

> [!PROBLEM] mount: /mnt/boot: wrong fs type on /dev/sda1.
> That partition is not the FAT partition the tool expects. Run mkfs.fat -F 32 /dev/sda1 first, and check the name.

> [!PROBLEM] mount: /mnt: wrong fs type, bad option, bad superblock on /dev/sda2.
> The root is not formatted as ext4 yet. Run mkfs.ext4 /dev/sda2 first.

> [!PROBLEM] mount: /mnt: mount point does not exist.
> You tried to bind a folder before mounting the root. Mount the root first, then bind.

> [!PROBLEM] mount: /mnt/data: mount point does not exist.
> That folder is not /mnt, /mnt/gentoo, or their boot or efi folder. Use one of those.

> [!SEE] jsc:arch_chroot, jsc:arch_fstab, jsc:when_a_tool_refuses
