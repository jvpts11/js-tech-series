---
section: midsoft_installs
order: 200
icon: jsc:cd_rom
---
# Two systems on one computer

Why choose? Install a second system on another disk of the same computer, and you have both.

Unless the server's boot menu is turned off, each start of a computer with two systems stops at a boot manager, so you pick which system to start: the Midsoft Boot Manager on Frames, GRUB on Linux, FreeBSD's own loader. With one system only, a Frames machine goes straight through ([](jsc:midsoft_boot_manager)). The boot order in the firmware sets which disk the firmware tries first ([](jsc:firmware)).

The installer proposes a default disk for the new system: the first disk with no system on it. When every disk has a system, it proposes the first disk. A disk holds up to 16 systems, as long as it has room for each one.

The new system becomes the one the computer boots by default. Installing a system that the disk already carries does not add a second copy: the setup rewrites the system's boot loader, which is how a damaged system is repaired.

A server setting can turn the boot menu off, so the computer boots its default system at once, with no list. The setting is boot.show_boot_menu.

> [!DEFINE] Boot manager
> The program that lists the systems on a computer, so you can pick one to start.

> [!DEFINE] Midsoft Boot Manager
> The list of systems that a Frames machine shows when it starts, so you can pick one.

> [!DEFINE] Boot order
> The order the firmware tries the disks in when the computer starts. The first disk with a system on it starts.

## If something goes wrong

> [!PROBLEM] The computer starts into the wrong system.
> Pick the system you want at the Midsoft Boot Manager, or change the boot order in the firmware ([](jsc:firmware)).

> [!PROBLEM] The screen says "The target disk has no room for", then the name of the second system and the megabytes it needs.
> Install it on another disk with room, or on a bigger disk ([](jsc:disks)).

> [!PROBLEM] The computer starts at once, with no list, although it has two systems.
> The server's boot menu setting is off. Turn it on, with the setting boot.show_boot_menu.

> [!SEE] jsc:midsoft_boot_manager, jsc:installing, jsc:firmware
