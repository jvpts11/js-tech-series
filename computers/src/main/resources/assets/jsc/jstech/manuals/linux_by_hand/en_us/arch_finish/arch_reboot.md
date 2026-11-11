---
section: arch_finish
order: 316
icon: jsc:cd_rom
---
# Checking and rebooting

Before it reboots, the installer checks that the new system can start. If a part is missing, the machine lists what is missing, and it refuses to reboot until you add the part.

When the check passes, the installer copies the new system onto the disk that holds /mnt. Then the machine reboots into it. The copy also takes the file system table you wrote, the name you chose, and the programs you asked for with pacman -S.

1. Type exit and press [[Enter]]. The prompt shows the live medium again.
2. Type reboot and press [[Enter]].

Table: What the check looks for

| Missing part | The message it prints |
| --- | --- |
| The base system | `no base system installed (pacstrap)` |
| The file system table | `no filesystem table (genfstab -U /mnt >> /mnt/etc/fstab)` |
| The boot loader | `no bootloader (grub-install)` |
| The boot list | `nothing for the bootloader to start (grub-mkconfig -o /boot/grub/grub.cfg)` |

The boot images are not on this list. If the server setting Every step of Arch is on, the list also checks the hardware clock and the machine's name ([](jsc:arch_clock_and_name)).

If a part is missing, the reboot stops and lists what is missing:

```session
{"lines": [{"prompt": "root@archiso ~ # ", "text": "reboot", "typed": true}, {"text": "The new system will not boot:"}, {"text": "  - no bootloader (grub-install)"}, {"text": "  - nothing for the bootloader to start (grub-mkconfig -o /boot/grub/grub.cfg)"}], "look": "jsc:guide/terminal/zsh", "title": "Arch Linux"}
```

After a refusal, do each listed step, in this chapter, and then reboot again. The firmware starts the installed disk first, and the boot list shows the entry Arch Linux.

## If something goes wrong

> [!PROBLEM] The screen says "reboot: you are inside the new system. exit first."
> You are still inside the new system. Type exit, then reboot.

> [!PROBLEM] The screen says "The new system will not boot:" and lists the missing parts.
> Do each listed step, then reboot again.

> [!PROBLEM] The screen says "The installation could not be written to the disk (no space or no disk)."
> The disk that holds /mnt has no room for the system, or it is not there. Check the disk, then install again.

> [!SEE] jsc:when_reboot_refuses
