---
section: arch_finish
order: 317
icon: jsc:cd_rom
---
# Installing packages with pacman

Inside the new system, pacman -S installs programs from the Mirror, so that they are on the new system when it starts. A program you install this way is a package, with its files and the names of what it needs.

Type pacman -S, a space, and then the names of the programs you want. Each name is the command name of a program that the Mirror has for Linux.

1. Make sure you are inside the new system.
2. Type pacman -S and the names, for example pacman -S grub efibootmgr.
3. Answer the question with Y or press [[Enter]] to install, or type n to stop.

Pacman asks "Proceed with installation? [Y/n]" before it changes anything. An answer that starts with n stops it, and nothing is installed. Any other answer, even an empty one, goes on.

```session
{"lines": [{"prompt": "[root@archiso /]# ", "text": "pacman -S efibootmgr", "typed": true}, {"text": ":: Proceed with installation? [Y/n]"}], "look": "jsc:guide/terminal/zsh", "title": "Arch Linux"}
```

Each program you name is remembered, and it is installed on the new system when the install finishes. The package grub is also what the boot loader needs ([](jsc:arch_bootloader)).

## pacman on the system you installed

Once Arch Linux starts from your own disk, pacman stays as its package manager. Then pacman -S installs a package at once, and pacman has words to search, list, update and remove your packages too. Searching, installing and updating reach the Mirror over the network, while listing and removing work on the packages already on this computer.

Table: What pacman does on the installed system

| Words | What they do |
| --- | --- |
| `pacman -S <package>` | Installs the package from the Mirror. |
| `pacman -Ss [term]` | Searches the Mirror for packages whose names hold the term. |
| `pacman -Q` | Lists the packages installed on this computer. |
| `pacman -Sy` | Reads the Mirror's lists again. |
| `pacman -Syu` | Brings every installed package up to the version the Mirror serves. |
| `pacman -R <package>` | Removes the package. |
| `pacman -Qu` | Lists the installed packages the Mirror has a newer version of, each with the version you have and the new one. |

```command
id: jsc:command/linux/pacman
```

## If something goes wrong

> [!PROBLEM] The screen says "error: target not found:" and then the name you typed.
> The Mirror has no program by that name for Linux, or this machine cannot run it. Check the spelling, and look at the programs on the Mirror ([](jsc:programs)).

> [!PROBLEM] The screen says "error: no targets specified (use -h for help)".
> You typed only flags, and no name. Add the name of a program after -S.

> [!PROBLEM] The screen says "error: no operation specified (use -h for help)".
> The command must start with -S and name a program. Type pacman -S, a space, then the name.

> [!PROBLEM] The screen says "error: you cannot perform this operation unless you are root in the new" and then "system (arch-chroot /mnt first)".
> You are on the live medium. Step in first with arch-chroot /mnt ([](jsc:arch_chroot)).

> [!PROBLEM] The screen says "error: failed retrieving file 'core.db' from mirror://mainframe : Could not resolve host: mainframe".
> This computer cannot reach a Mirror. Put it on a network whose Mainframe runs the Mirror ([](jsc:mirror)).

> [!SEE] jsc:programs
