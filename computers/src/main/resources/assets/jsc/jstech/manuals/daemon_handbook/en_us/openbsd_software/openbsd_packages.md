---
section: openbsd_software
order: 345
---
# Packages and the ports tree

OpenBSD's package manager is three commands. pkg_add fetches a package from the network's Mirror and installs it, pkg_info lists what is installed, and pkg_delete takes a package off.

Table: The three tools

| Command | What it does |
| --- | --- |
| `pkg_add <package>` | Installs a package from the Mirror. |
| `pkg_add -u` | Brings every installed package up to the Mirror's version. |
| `pkg_info` | Lists the installed packages. |
| `pkg_info -Q <term>` | Searches the Mirror. |
| `pkg_delete <package>` | Takes a package off. |
| `pkg_add -un` | Lists what pkg_add -u would bring up, each package with its two versions, and installs nothing. |

```command
id: jsc:command/openbsd/pkg_add
```

```command
id: jsc:command/openbsd/pkg_info
```

```command
id: jsc:command/openbsd/pkg_delete
```

## The ports tree

The ports tree is the recipe of every program, to build on your own computer. On OpenBSD you fetch it with cvs, and build a port with make, which needs the comp set.

1. Type cvs -q checkout -P ports to lay the tree out under /usr/ports.
2. Go to a port's folder, such as cd /usr/ports/sysutils/screenfetch.
3. Type make install clean to build it, install it and clean up.
4. Later, type cvs -q update -Pd in /usr/ports to bring the tree up to date.

```command
id: jsc:command/openbsd/cvs
```

> [!SEE] jsc:openbsd_patches, jsc:openbsd_overview
