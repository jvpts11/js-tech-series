---
section: desktop_environments
order: 263
icon: jsc:monitor
---
# Installing a desktop

On Linux and FreeBSD you have two ways to get a desktop. You can choose one while the system installs, or add one afterwards from the terminal. Both ways read the Mirror, the network's package shelf ([](jsc:mirror)).

A Mirror answers when your computer reaches a Mainframe that runs it, through a data cable. The Mirror is a service, so it stops answering when the cable is cut.

## At install time

The installer of Ubuntu, Debian, Fedora and FreeBSD has a Desktop page. Gentoo, Arch Linux, UNIX System V and MC-DOS have none. The Desktop page appears only when a Mirror answers.

1. Connect the computer to a network whose Mainframe runs the Mirror.
2. Start the installer of Ubuntu, Debian, Fedora or FreeBSD.
3. On the Desktop page, pick a desktop, or the terminal only.
4. Let the installer finish. A desktop you chose is installed as a step of its own.
5. Restart the computer, so the desktop starts.

## After the install

Once the system runs, you add a desktop from its terminal. Use the install form of the system's package manager, and give the desktop's name ([](jsc:software_commands)).

Table: The install forms

| Package manager | Systems | Form |
| --- | --- | --- |
| apt | Debian, Ubuntu | `apt install <name>` |
| dnf | Fedora | `dnf install <name>` |
| pacman | Arch Linux | `pacman -S <name>` |
| emerge | Gentoo | `emerge <name>` |
| pkg | FreeBSD | `pkg install <name>` |

The name is the desktop's command name: kde-plasma, gnome, cinnamon or cde. The id works too, as kde_plasma. Capitals do not matter.

Gentoo builds a desktop from source in the terminal, and the terminal waits until the build is done.

Which desktops the installer offers depends on the system and on your computer's era.

On a Legacy computer it offers KDE Plasma, GNOME and CDE, where the system runs them. A Standard computer is also offered Cinnamon. A Vintage computer is offered none.

UNIX System V installs its programs from its install media, and has no package manager. A desktop is not added from the Mirror on UNIX.

## If something goes wrong

> [!PROBLEM] The Desktop page does not appear.
> No Mirror answers, or the installer offers no desktop. Connect to a network whose Mainframe runs the Mirror, or add a desktop after the install.

> [!PROBLEM] could not resolve mirror:// - connect this computer to a network whose Mainframe runs the Mirror service
> Connect the computer to a network whose Mainframe runs the Mirror, then try again.

> [!PROBLEM] The package manager says "unable to locate package" and the name.
> The name is not a package this system is offered. Check the name: kde-plasma, gnome, cinnamon or cde.

> [!PROBLEM] cinnamon needs Standard hardware or later
> The computer is older than the desktop. Use a computer of that era or a newer one.

> [!PROBLEM] unmet requirements (hardware or free disk space)
> The disk has less free room than the desktop takes, or the hardware is short. Free some disk space, then try again.

> [!PROBLEM] This computer cannot store installed programs.
> The computer has no disk to keep programs on. Give it one.

> [!PROBLEM] The message "This computer is still setting up" and a desktop's name.
> A previous install is still running. Wait until it finishes.

> [!PROBLEM] Setup cancelled. Nothing was installed.
> The setup stopped, so the desktop was not added. Install it again, as the steps above show.

> [!PROBLEM] The package manager says "<name> is already the newest version".
> The desktop is installed already. Restart the computer, because it starts at the next boot.

> [!SEE] jsc:desktops, jsc:the_systems, jsc:installing, jsc:mirror
