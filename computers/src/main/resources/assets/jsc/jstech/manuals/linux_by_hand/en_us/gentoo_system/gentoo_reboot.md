---
section: gentoo_system
order: 326
---
# Rebooting into Gentoo

The reboot is the last step. Before it starts the new system, the machine checks that the system has everything it needs to boot. If you missed something, it lists the missing items and refuses to start ([](jsc:reboot_checklist)).

1. Type exit and press [[Enter]].
2. Type cd and press [[Enter]].
3. Type umount -R /mnt/gentoo and press [[Enter]]. The command prints nothing.
4. Type reboot and press [[Enter]].

The machine always checks the items in the first table. Each missing item prints with its fix in brackets.

Table: Always checked

| Missing item | What the machine says |
| --- | --- |
| No base | no base system installed (wget the stage 3, then tar) |
| No root in fstab | no root filesystem in /etc/fstab (blkid, then write the line) |
| No kernel | no kernel (genkernel all, or make in /usr/src/linux) |
| No bootloader | no bootloader (grub-install) |
| No GRUB menu | nothing for the bootloader to start (grub-mkconfig -o /boot/grub/grub.cfg) |

With install_by_hand.gentoo_every_step set to true, the machine also checks the items in the second table.

Table: Checked when every step is asked

| Missing item | What the machine says |
| --- | --- |
| Binds | /proc, /sys, /dev and /run were not bound into the new system (mount --rbind, --types proc) |
| Profile | no profile chosen (eselect profile list, then set) |
| World | the system was not brought up to date (emerge --ask --verbose --update --deep --newuse @world) |
| Kernel link | /usr/src/linux points nowhere (eselect kernel set 1) |
| Name | the machine has no name (echo <name> > /etc/hostname) |

The name is set inside the new system, with echo <name> > /etc/hostname or with hostname <name>. A name starts with a letter or digit, holds up to 15 characters, and may contain hyphens.

When every item is in place, the reboot makes the disk you chose boot first. The machine answers "Rebooting into the new system".

## If something goes wrong

> [!PROBLEM] reboot says "you are inside the new system. exit first."
> Type exit to leave the new system, then type reboot again.

> [!PROBLEM] The machine says "The new system will not boot" and lists items.
> Each item names its fix in brackets. Do that step, then type reboot again.

> [!PROBLEM] hostname says "the specified hostname is invalid".
> Use up to 15 letters, digits and hyphens, and start with a letter or digit.

> [!PROBLEM] hostname says "you cannot change the host name of the live medium".
> Step into the new system first with chroot /mnt/gentoo /bin/bash, then set the name.

> [!SEE] jsc:reboot_checklist, jsc:will_not_boot_messages, jsc:after_reboot_checks
