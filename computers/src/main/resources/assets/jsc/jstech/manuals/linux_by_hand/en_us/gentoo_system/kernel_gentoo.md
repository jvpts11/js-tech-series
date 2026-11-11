---
section: gentoo_system
order: 323
---
# The kernel

The kernel is the part of the system that drives the hardware. Every program reaches the processor, the memory and the disks through it. Gentoo builds your kernel from its sources, so the install downloads the sources first.

This install uses kernel version 6.11.5. Its files carry the name 6.11.5-gentoo, and the compressed kernel goes into /boot.

1. Type emerge sys-kernel/gentoo-sources and press [[Enter]].
2. Type eselect kernel list and press [[Enter]] to see the kernels.
3. Type eselect kernel set 1 and press [[Enter]]. The command prints nothing.
4. Build and install the kernel by one of the two routes in the table below.

genkernel is a script that builds the whole kernel with one command. The by-hand route runs in /usr/src/linux, the folder that the kernel choice makes.

> [!DEFINE] genkernel
> A script that builds the kernel and its modules with one command.

Table: The two routes

| Route | Command | What it does |
| --- | --- | --- |
| Automatic | `genkernel all` | Builds the kernel and its modules, and copies the kernel into /boot. |
| By hand | `make -j4 && make modules_install && make install` | Builds in /usr/src/linux, installs the modules, and copies the kernel into /boot. |

The modules live under /lib/modules, in a folder named for the kernel version.

> [!DEFINE] module
> A piece of the kernel that loads when something needs it, kept as its own file.

## If something goes wrong

> [!PROBLEM] genkernel says "command not found", because the command is on the new system and you are still in the live medium.
> Step into the new system first: chroot /mnt/gentoo /bin/bash. make needs the same step.

> [!PROBLEM] genkernel says "\* ERROR: no kernel sources found in /usr/src".
> Download the sources first: emerge sys-kernel/gentoo-sources.

> [!PROBLEM] eselect says "!!! Error: No kernel sources found in /usr/src".
> Download the sources first: emerge sys-kernel/gentoo-sources.

> [!PROBLEM] make says it has no targets and no makefile.
> The kernel is built in its sources. Type cd /usr/src/linux first.

> [!PROBLEM] make says it has no rule for the target vmlinux.
> The kernel is not built yet. Type make first, then the install.

> [!SEE] jsc:fstab_by_hand, jsc:gentoo_bootloader, jsc:package_tree_and_world
