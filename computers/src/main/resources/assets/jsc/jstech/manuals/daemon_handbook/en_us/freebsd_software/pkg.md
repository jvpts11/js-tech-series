---
section: freebsd_software
order: 336
---
# pkg, verb by verb

pkg is the package manager of FreeBSD. A package is a program built by someone else and fetched ready to install, so you do not build it yourself. The packages come from the network's Mirror, and you type pkg, a verb, and the name it works on.

> [!DEFINE] Community package
> A package another player published to the Mirror.

> [!DEFINE] Outdated
> An installed package whose version differs from the one the Mirror serves now.

Table: The verbs of pkg

| Command | What it does |
| --- | --- |
| `pkg install <package>` | Fetches a package from the Mirror and installs it. |
| `pkg delete <package>` | Removes an installed package. |
| `pkg search [term]` | Lists the packages that match the term, one row each. |
| `pkg info` | Lists every installed package, with its description. |
| `pkg update` | Refreshes the Mirror's list of packages without changing any installed package. |
| `pkg upgrade` | Installs newer versions of the packages you have installed. |
| `pkg version -vRL=` | Lists the installed packages that need updating, each with the version the Mirror has. |

Three shorter words also work: add means install, remove means delete, and query means info.

pkg search marks a package [installed] when it is on your computer, and [community] when another player published it. pkg upgrade installs the newer version of each Outdated package.

```command
id: jsc:command/freebsd/pkg
```

## Installing a package

1. Type pkg install and the name of the package.
2. Read the lines that name the packages and the download size.
3. Wait while the package is set up. The setup runs as a job you can watch.

```listing
{"lines": ["Updating Mirror repository catalogue...", "Mirror repository is up to date.", "The following 1 package(s) will be affected (of 0 checked):", "New packages to be INSTALLED:", "        <name>: <version>", "Number of packages to be installed: 1", "<n> MiB to be downloaded.", "[1/1] Fetching <name>-<version>.pkg from mirror://mainframe"], "name": "pkg install"}
```

A delete lists the packages it removes, then says how much space it frees, such as "The operation will free <n> MiB."

## Messages of pkg

Table: Messages of pkg

| Message | What it means |
| --- | --- |
| `pkg: could not resolve mirror://` | This computer is not on a network whose Mainframe runs the Mirror service. |
| `pkg: unable to locate package <name>` | The Mirror does not offer a package with that name. |
| `pkg: unmet requirements (hardware or free disk space)` | The computer lacks the hardware or the free disk space the package needs. |
| `pkg: <name> is not installed.` | You tried to delete a package that is not installed. |
| `pkg: <name> comes with every system.` | The program is already there, so there is nothing to install. |
| `pkg: <name> is already installed.` | The package is on the computer already. |
| `pkg: <name> needs <era> hardware or later` | The computer is of an older era than the package needs. |
| `pkg: <name> only installs on the Mainframe` | The package runs only on that kind of computer: the Mainframe, a server in a rack, a Cluster Management Computer or a Personal Computer. |
| `pkg: this computer cannot store installed programs` | The computer has no disk to keep programs on. |
| `no packages installed` | pkg info has nothing to list. |
| `no packages match '<term>'` | pkg search found no package with that word. |

## If something goes wrong

> [!PROBLEM] pkg says it could not resolve mirror://.
> Connect this computer to a network whose Mainframe runs the Mirror service.

> [!PROBLEM] pkg says it cannot locate the package by its name.
> Type pkg search and a word to find the real name, then use that name.

> [!PROBLEM] pkg says the requirements are unmet.
> Free some disk space, or use a computer with better hardware.

> [!PROBLEM] pkg says the package is not installed when you delete it.
> Type pkg info to find the exact name.

> [!PROBLEM] pkg says the program comes with every system.
> Nothing to do: it is already on the computer.

> [!SEE] jsc:programs, jsc:software_commands, jsc:ports_tree
