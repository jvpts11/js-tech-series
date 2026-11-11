---
section: by_hand_basics
order: 299
icon: jsc:cd_rom
---
# Installing by hand

Most systems install themselves. You put their medium in a drive, boot from it, and the installer does the work while it asks you a few questions. Arch Linux and Gentoo do not work that way. You type each step of their installation yourself, in order, and the system answers the way the real tool would.

The install medium is the floppy disk, compact disc (CD), digital versatile disc (DVD) or universal serial bus (USB) stick that a system is installed from. For these two systems it is a CD. When you boot it, the CD runs its own shell, a line where you type commands, and that running CD is the live medium.

> [!DEFINE] Install medium
> The floppy disk, compact disc (CD), digital versatile disc (DVD) or universal serial bus (USB) stick a system is installed from.

> [!DEFINE] Live medium
> An install medium that is running with its own shell. The shell comes from the medium, not from your disk, and nothing is installed until you type the steps.

The shell answers with the real tools' words, and it refuses what a real machine would refuse at that step. A mistake looks like a mistake on a real computer, so you learn the same reasons the real systems give.

Table: The two systems installed by hand

| System | Shell on the medium | How it is built |
| --- | --- | --- |
| Arch Linux | `zsh` | The base system is installed from the network's package repository. |
| Gentoo | `bash` | The base system and the kernel are compiled from source first, which takes time. |

> [!DEFINE] Arch Linux
> A Linux system you install by hand from its live medium, taking its base system from the network's package repository.

> [!DEFINE] Gentoo
> A Linux system you install by hand from its live medium, compiling its base system and its kernel from source.

The install ends when you type reboot. The shell refuses the reboot until the new system has everything it needs to start, and it lists what is missing. How long the steps take depends on your machine: a slow machine takes longer to compile (Gentoo) and to unpack (Arch Linux).

Before you boot the medium, check what you need ([](jsc:what_you_need)).

## If something goes wrong

> [!PROBLEM] You expect an installer to start on its own, and a shell opens instead.
> That shell is the live medium. Nothing is installed yet: read the walkthrough and type its steps, starting with help ([](jsc:the_walkthrough)).

> [!SEE] jsc:installing, jsc:the_systems
