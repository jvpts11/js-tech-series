---
section: sharing_packages
order: 497
---
# Installing a package other players published

Players can publish packages for anyone on the network. You install one like any other program: with the install command of your system's package manager and the package's name, such as apt install mypkg on a Debian or Ubuntu system ([](jsc:software_commands)).

Installing a player's package needs the Sigma runtime on your computer ([](jsc:sigma_installing)). Without it, the install stops and says to install sigma first.

The game unpacks each package into a folder of its own: PROGRAMS, then the package's name. The folder holds only that package's files, so two packages cannot overwrite each other. The package then shows on the desktop with its icon, and opening it runs the program.

1. Type the install command of your package manager, with the package's name.
2. Open the program from the desktop.

To remove a package, type the remove command of your package manager, with its name, such as apt remove mypkg. The game deletes exactly the files the install wrote, then the folder, and answers removed mypkg.

## If something goes wrong

> [!PROBLEM] The install says this system installs programs from install media, not a package manager.
> Install the program from the install media of the system instead ([](jsc:installing)).

> [!PROBLEM] The install says the Mirror's copy of this package is not readable.
> Ask the player who published it to publish it again from a built file.

> [!PROBLEM] The install says mypkg is a Σ# program; install sigma first.
> Install the Sigma runtime on this computer ([](jsc:sigma_installing)), then install again.

> [!PROBLEM] The install says no system disk to install onto.
> Put a system disk in the computer, then install again.

> [!PROBLEM] The install says this system has no folders to install into.
> Check the system disk, then install again.

> [!SEE] jsc:software_commands, jsc:sigma_installing, jsc:sigma_sharing_errors
