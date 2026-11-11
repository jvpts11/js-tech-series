---
section: arch_system
order: 312
icon: jsc:cd_rom
---
# The clock and the machine's name

Two settings make the new system yours: the clock and the machine's name. The installer asks for both before the reboot only when the server setting Every step of Arch is on ([](jsc:installing)). You can always set them with the commands below.

The command hwclock --systohc copies the system's clock into the hardware clock, so the time is kept while the computer is off.

> [!DEFINE] hardware clock
> The clock on the motherboard, which keeps the time while the computer is off.

1. Type hwclock --systohc and press [[Enter]].
2. Type echo jarch > /etc/hostname, with your own name in place of jarch, and press [[Enter]].
3. Type hostname and press [[Enter]]. It prints the name you chose.

Table: The rules for the machine's name

| Rule | What it says |
| --- | --- |
| Length | At most 15 characters. |
| First character | A letter or a digit. |
| Other characters | Letters, digits and hyphens. |
| Not allowed | Dots, underscores and spaces. |

The name goes with the new system when the install finishes. The installed machine keeps it, and the network shows it.

## If something goes wrong

> [!PROBLEM] The screen says "hostname: you cannot change the host name of the live medium".
> You are on the live medium, not in the new system. Step in with arch-chroot /mnt first ([](jsc:arch_chroot)).

> [!PROBLEM] The screen says "hostname: the specified hostname is invalid".
> The name breaks a rule in the table, or it has more than one word. Use letters, digits and hyphens, at most 15 characters, starting with a letter or a digit.

> [!SEE] jsc:arch_mkinitcpio
