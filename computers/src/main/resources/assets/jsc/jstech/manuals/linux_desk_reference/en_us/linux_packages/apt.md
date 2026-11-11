---
section: linux_packages
order: 291
---
# apt, verb by verb

apt is the package manager of Ubuntu and Debian. You type apt, a verb such as install, then the name of what it works on, and it fetches from the Mirror. On Fedora the word is not a command: use dnf ([](jsc:dnf)).

Words that start with a dash are skipped, so apt install -y vim installs vim.

Table: The apt verbs

| Command | What it does |
| --- | --- |
| `apt install <package>` | Looks the package up on the Mirror and installs it. |
| `apt remove <package>` | Removes an installed package. purge and erase do the same. |
| `apt search [term]` | Lists the packages on the Mirror whose name or text matches the term. Installed ones are tagged. |
| `apt list` | Lists the installed packages, with their names and descriptions. |
| `apt update` | Reads the package lists from the Mirror. It installs nothing. |
| `apt upgrade` | Upgrades installed packages to the Mirror's versions; full-upgrade and dist-upgrade do the same. |
| `apt list --upgradable` | Lists the installed packages the Mirror has a newer version of, each with the version you have. It installs nothing. |

```command
id: jsc:command/linux/apt
```

## Installing a program

1. Type apt search and a word from the program's name, then press ENTER.
2. Find the command name in the first column of the list.
3. Type apt install and the command name, then press ENTER.
4. Wait until the progress bar of the install is full.

An install prints these lines, in this order, before its progress bar.

Table: What apt prints for an install

| Line | What it means |
| --- | --- |
| `Reading package lists... Done` | The package lists are read. |
| `Building dependency tree... Done` | The packages the program needs are worked out. |
| `The following NEW packages will be installed:` | The program's name follows on the next line. |
| `Need to get <MB> MB of archives.` | The size of the download. |
| `Get:1 <mirror address> stable/main <package> <version> [<MB> MB]` | The download from the Mirror. |

A remove prints the same first two lines. Then it prints "The following packages will be REMOVED:" and "After this operation, <MB> disk space will be freed."

## If something goes wrong

> [!PROBLEM] command not found: apt
> This is Fedora, which uses dnf. Type dnf in its place ([](jsc:dnf)).

> [!PROBLEM] E: could not resolve mirror:// - connect this computer to a network whose Mainframe runs the Mirror service
> The computer is not on a network whose Mainframe runs the Mirror. Connect it, as [](jsc:mirror_and_packages) explains.

> [!PROBLEM] E: unable to locate package <name>
> The name is wrong, or the program is not offered on Linux. Find the right name with apt search.

> [!PROBLEM] E: <name>: unmet requirements (hardware or free disk space)
> The window says how much disk is missing. Free up disk space, or use a bigger disk.

> [!PROBLEM] E: cinnamon needs Standard hardware or later
> The computer is a Legacy machine. Use a Standard computer, or install KDE Plasma or GNOME.

> [!PROBLEM] <name> is already the newest version
> Nothing to do. To bring every installed package up to date, use apt upgrade.

> [!PROBLEM] This computer is still setting up <name>.
> A computer runs one install at a time. Wait for the first one to finish.

> [!PROBLEM] <name> is not installed.
> A remove found nothing to remove. Check the installed list with apt list.

> [!SEE] jsc:dnf, jsc:mirror_and_packages, jsc:what_is_missing
