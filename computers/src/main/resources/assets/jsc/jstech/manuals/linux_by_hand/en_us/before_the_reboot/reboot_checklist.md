---
section: before_the_reboot
order: 328
---
# What the new system needs before the reboot

When the new system is installed, you restart the machine into it with the reboot command. Before it restarts the machine, the shell checks a short list of what the new system needs to start. If something on the list is missing, the machine does not restart: the shell names the missing items, and your live session stays open.

The reboot step of each guide ([](jsc:arch_reboot), [](jsc:gentoo_reboot)) runs this check. The list has two parts. The always-asked part holds the steps that every installation needs. The optional part holds the rest of the handbook, and the shell checks it only when a setting asks for it.

## Checking before the reboot

1. Type reboot and press [[Enter]].
2. If the shell prints "The new system will not boot:", read the lines under it.
3. Do the step that each line names.
4. Type reboot again.

## The always-asked part

Each row names one missing item, the line the shell prints for it, and the step that clears it.

Table: Always asked for

| Missing item | The shell prints | The step that clears it |
| --- | --- | --- |
| Base system (Gentoo) | `no base system installed (wget the stage 3, then tar)` | Unpack the stage 3 archive onto the disk ([](jsc:stage_three)). |
| Base system (Arch) | `no base system installed (pacstrap)` | Install the base system with pacstrap ([](jsc:arch_base_system)). |
| Table of what to mount (Gentoo) | `no root filesystem in /etc/fstab (blkid, then write the line)` | Write the line for the root partition ([](jsc:fstab_by_hand)). |
| Table of what to mount (Arch) | `no filesystem table (genfstab -U /mnt >> /mnt/etc/fstab)` | Write the table with genfstab ([](jsc:arch_fstab)). |
| Kernel (Gentoo) | `no kernel (genkernel all, or make in /usr/src/linux)` | Build and install the kernel ([](jsc:kernel_gentoo)). |
| Bootloader (both) | `no bootloader (grub-install)` | Install GRUB ([](jsc:arch_bootloader) on Arch, [](jsc:gentoo_bootloader) on Gentoo). |
| Bootloader's list (both) | `nothing for the bootloader to start (grub-mkconfig -o /boot/grub/grub.cfg)` | Write the list with grub-mkconfig ([](jsc:arch_grub_config)). |

Gentoo checks its table by the content of the file, not by running a tool. A line that does not start with a hash sign needs at least three fields, and its second field must be a slash. The hint names blkid, the tool that shows each partition's details. The stage 3 writes a table with only comments in it, so this item stays on the list until you write the root line. On Arch the item goes as soon as genfstab has run.

## The optional part

The shell asks for the optional part only when the world's setting for that system is on. Both settings are off by default, so a world asks for the basics alone. The settings are install_by_hand.gentoo_every_step and install_by_hand.arch_every_step, in the server configuration.

Table: The optional part, when its setting is on

| Missing item | The shell prints | The step that clears it |
| --- | --- | --- |
| Bind mounts (Gentoo) | `/proc, /sys, /dev and /run were not bound into the new system (mount --rbind, --types proc)` | Bind all four folders into the new system ([](jsc:mounting)). |
| Profile (Gentoo) | `no profile chosen (eselect profile list, then set)` | Choose a profile ([](jsc:package_tree_and_world)). |
| World update (Gentoo) | `the system was not brought up to date (emerge --ask --verbose --update --deep --newuse @world)` | Run the update the line names ([](jsc:package_tree_and_world)). |
| Kernel link (Gentoo) | `/usr/src/linux points nowhere (eselect kernel set 1)` | Point /usr/src/linux at a kernel with eselect kernel set ([](jsc:kernel_gentoo)). |
| Hardware clock (Arch) | `the hardware clock was not set (hwclock --systohc)` | Set the hardware clock with hwclock ([](jsc:arch_clock_and_name)). |
| Machine name (both) | `the machine has no name (echo <name> > /etc/hostname)` | Give the machine a name, as the section below explains. |

## The machine's name

A name has 1 to 15 characters. It uses letters, digits and hyphens, and it starts with a letter or a digit. You give it with the hostname command, or by writing it to /etc/hostname inside the new system. Either way, the checklist counts it.

> [!DEFINE] Reboot checklist
> The list of what the new system still needs, which the live shell builds before it restarts the machine.

## If something goes wrong

> [!PROBLEM] hostname: the specified hostname is invalid
> Use 1 to 15 letters, digits or hyphens, and start with a letter or a digit.

> [!PROBLEM] hostname: you cannot change the host name of the live medium
> Enter the new system first ([](jsc:arch_chroot) on Arch, [](jsc:mounts_and_chroot) on Gentoo), then set the name there.
