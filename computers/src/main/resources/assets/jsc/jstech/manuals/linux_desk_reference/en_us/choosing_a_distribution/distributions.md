---
section: choosing_a_distribution
order: 285
---
# What a distribution is, and which to choose

A Linux system comes in many versions, and each version is called a distribution. A distribution bundles the Linux kernel, the programs that run on it, its own installer and its own package manager. Here you can install three of them: Ubuntu, Debian and Fedora.

Each of the three is a Legacy system: it takes Legacy hardware or newer. All three boot the same kernel, named 6.8-jsc, and they differ in the disk they take, the random-access memory (RAM) they hold while they run, and the package manager they use. The numbers below are the game's own, not the exact requirements of a real machine.

```table
{"caption": "The three distributions", "heads": ["Distribution", "Disk it takes", "RAM while it runs", "Package manager"], "rows": [[{"words": "Ubuntu"}, {"amount": 8192, "unit": "MB"}, {"amount": 48, "unit": "MB"}, {"literal": "apt"}], [{"words": "Debian"}, {"amount": 4096, "unit": "MB"}, {"amount": 24, "unit": "MB"}, {"literal": "apt"}], [{"words": "Fedora"}, {"amount": 8192, "unit": "MB"}, {"amount": 48, "unit": "MB"}, {"literal": "dnf"}]]}
```

The disk figure is the room the system takes for itself, before any program or desktop. A disk with less free room than that cannot take the system: see [](jsc:installing_linux_keys).

A distribution starts at its text console, the screen with a prompt. A desktop, the graphical screen with windows and menus, is a package you add afterwards. The three desktops are in the table below.

```table
{"caption": "Desktops you can add", "heads": ["Desktop", "Command", "Disk to install", "RAM while it runs"], "rows": [[{"words": "KDE Plasma"}, {"literal": "kde-plasma"}, {"amount": 256, "unit": "MB"}, {"amount": 224, "unit": "MB"}], [{"words": "GNOME"}, {"literal": "gnome"}, {"amount": 192, "unit": "MB"}, {"amount": 256, "unit": "MB"}], [{"words": "Cinnamon"}, {"literal": "cinnamon"}, {"amount": 160, "unit": "MB"}, {"amount": 160, "unit": "MB"}]]}
```

KDE Plasma and GNOME need Legacy hardware or newer. Cinnamon needs Standard hardware or newer, so a Legacy computer cannot take it. The installers offer a desktop only when a Mirror answers, as [](jsc:mirror_and_packages) explains.

The table gives the numbers to compare. Debian takes the least disk and holds the least memory, while Ubuntu and Fedora take the most. The package manager is the other difference: apt on Ubuntu and Debian, dnf on Fedora.

> [!DEFINE] Distribution
> One Linux system, packaged with its own installer, its own package manager and its own sizes.

> [!DEFINE] Package manager
> The command that fetches and installs programs for a distribution, from the Mirror.

> [!DEFINE] Mirror
> The package server on the network's Mainframe. Installers and package managers fetch from it.

> [!DEFINE] Kernel
> The core of the system, the part that runs the programs and talks to the machine. Here it is named 6.8-jsc.

## If something goes wrong

> [!PROBLEM] The installer shows no desktop page.
> No Mirror answers, so the computer is not on a network whose Mainframe runs the Mirror. Connect it, then install ([](jsc:mirror_and_packages)).

> [!PROBLEM] Cinnamon is missing from the desktop list.
> The computer is a Legacy machine. Choose KDE Plasma or GNOME, or use a Standard computer.

> [!SEE] jsc:installing_linux_keys, jsc:install_ubuntu, jsc:apt, jsc:dnf
