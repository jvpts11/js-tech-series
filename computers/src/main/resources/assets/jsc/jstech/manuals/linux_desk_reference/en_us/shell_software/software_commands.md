---
section: shell_software
order: 259
---
# Installing and removing software

There are two ways to add software to a Unix system, and this entry says which is which. The store installs a program from an install disc in a drive joined to the computer. A package manager installs and removes packages from the Mirror.

A package manager works only on the system that uses it. Removing software on these systems is the package manager's job: there is no uninstall command here, because uninstall is a DOS command. If you are not sure how a command got here, type which, and it says whether the system or a package put it there.

Table: Package managers

| System | Manager | Its verbs |
| --- | --- | --- |
| Ubuntu and Debian | `apt` | install, remove, search, list, update and upgrade |
| Fedora | `dnf` | The same verbs as apt |
| Arch Linux | `pacman` | \-S to install, -R to remove, -Ss to search, -Q to list, -Syu to upgrade |
| Gentoo | `emerge` | A name to install, --unmerge to remove, --search, --sync and --update @world |
| FreeBSD | `pkg` | install, delete, search, info, update and upgrade |
| UNIX System V | `none` | No package manager on this system |

For the verbs of apt and dnf, read the Linux Desk Reference ([](jsc:apt)). For pkg, read the Daemon Handbook ([](jsc:pkg)). A package install or removal needs the Mirror, the network service that keeps the packages ([](jsc:mirror)).

## Installing from the store

1. Type store to list the programs you can install on this computer.
2. Put the install disc of the program in a drive joined to the computer.
3. Type install and the program's name, such as screenfetch.
4. Type programs to see the installed programs, and check that yours is there.

```command
id: jsc:command/linux/install
```

```command
id: jsc:command/linux/store
```

```command
id: jsc:command/linux/programs
```

The screenfetch program prints a short summary of the system, with labels such as OS, Kernel, Uptime, Packages and Shell. You install it first, like any program of the store.

> [!DEFINE] screenfetch
> A program that prints your system's logo beside a summary of the machine: its system, kernel, uptime, packages and shell.

```command
id: jsc:command/linux/screenfetch
```

## If something goes wrong

> [!PROBLEM] The install command says no such program.
> The store command lists the programs you can install.

> [!PROBLEM] The install command says the program needs its install disc in a linked drive.
> Put the disc in a drive joined to the computer, then run install again.

> [!PROBLEM] A package command says it could not resolve mirror.
> Connect the computer to a network whose [Mainframe](jsc:mainframes) runs the Mirror.

> [!PROBLEM] The shell says screenfetch: not found.
> Run install screenfetch first, then type screenfetch again.

> [!SEE] jsc:apt, jsc:dnf, jsc:arch_pacman_in_system, jsc:pkg
