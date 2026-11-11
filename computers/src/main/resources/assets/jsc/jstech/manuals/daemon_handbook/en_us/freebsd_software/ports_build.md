---
section: freebsd_software
order: 338
---
# Building ports: make, and the 10% lighter build

You build a port in its own folder, with the make command, which reads the port's recipe. The build compiles the program's source on this computer. It takes a while, and the result is made for this computer.

> [!DEFINE] Build
> Compiling the program's source on this computer.

> [!DEFINE] Distfile
> The source archive that a build fetches from the Mirror.

The usage line of make is: usage: make [build] [install | reinstall | deinstall] [clean].

1. Type cd and the port's folder, such as /usr/ports/sysutils/screenfetch.
2. Type make install clean to build, install and clean the port.
3. Type make deinstall to take the program off the computer again.

```command
id: jsc:command/freebsd/make
```

Table: The targets of make

| Target | What it does |
| --- | --- |
| `build` | Builds the port on this computer, which is what make does when you give no target. |
| `install` | Builds the port if it is not built, then installs it. |
| `reinstall` | Builds and installs the port over the one installed. |
| `deinstall` | Takes the installed port off the computer; with clean, it cleans first. |
| `clean` | Deletes the work folder that the build left. |

You combine targets in one line, such as make install clean. A build takes between 5 seconds and 30 minutes. It runs one job per processor core, and a faster processor shortens it.

## The 10% lighter build

A program you build on this computer needs 10% less than the package of the same program. When a build ends, it prints Built for this machine:, then a line such as 10% less memory, disk and processor than the package.

Table: What a build here saves

| What | Saved by a build here |
| --- | --- |
| Processor speed to run | 10% less |
| Disk space to install | 10% less |
| Memory while it runs | 10% less |
| Video memory | Not reduced |

## Messages of make

```table
{"caption": "Messages of make", "heads": ["Message", "What it means"], "rows": [[{"literal": "make: no target to make."}, {"words": "The folder is not a port's folder. make then stops in that folder."}], [{"literal": "make: don't know how to make <target>. Stop"}, {"words": "make does not know that target."}], [{"literal": "===>  <pkg> is already installed"}, {"words": "The port is on the computer already; the message suggests make deinstall and make install, or make reinstall."}], [{"literal": "===>  <pkg> is marked as broken: <reason>."}, {"words": "A gate of the install blocks the port, such as its era or the kind of computer."}], [{"literal": "===>  <pkg> cannot install: a Mainframe's services come prebuilt; use pkg install <name>."}, {"words": "The port is a Mainframe service, so install it with pkg instead."}], [{"literal": "===>  <pkg> cannot install: this computer cannot store installed programs."}, {"words": "The computer has no disk to keep programs on."}], [{"literal": "===>  <pkg> cannot install: this machine is short of the processor or the free disk space it needs."}, {"words": "The computer is too small for the port."}], [{"literal": "fetch: mirror://mainframe/distfiles/<distfile>: No address record"}, {"words": "The build needs its source, and no Mirror answers."}]]}
```

## If something goes wrong

> [!PROBLEM] make says there is no target to make.
> Go into the port's folder first, such as /usr/ports/sysutils/screenfetch.

> [!PROBLEM] make says the program is already installed.
> Type make reinstall, or make deinstall and then make install.

> [!PROBLEM] fetch says there is no address record for the Mirror.
> The Mirror cannot be reached. Connect the network to a Mirror, then try again.

> [!PROBLEM] make says the computer is short of the processor or the free disk space.
> Use a computer with a faster processor, or free some disk space.

> [!SEE] jsc:ports_tree, jsc:pkg, jsc:processors
