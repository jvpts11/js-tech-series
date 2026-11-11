---
section: gentoo_system
order: 327
---
# Building programs from source

Inside the new system, emerge installs programs the same way it did during the install. Portage compiles each program from its source on your machine, so every program takes time to build.

A compile is faster on a machine whose processor has a faster clock. The compiles at once are the smaller of two numbers: the count in MAKEOPTS ([](jsc:make_conf)), and the cores of your machine. There is always at least 1 compile at a time.

You name a program by its category and name, such as kde-plasma/plasma-meta, or by its name alone, in any letter case. The category follows the kind of program: applications go under app-misc, and desktops under x11-wm.

During the install, the Mirror installs by name only a program that runs on Linux, is not a background service, and fits the machine and its era.

To install a desktop, type emerge --ask kde-plasma/plasma-meta. It builds these packages in this order:

Table: What the KDE Plasma desktop brings

| Package | Version | Archive size |
| --- | --- | --- |
| `dev-qt/qtbase` | 6.7.3 | 48.0 MB |
| `kde-frameworks/kconfig` | 6.6.0 | 2.1 MB |
| `kde-frameworks/kcoreaddons` | 6.6.0 | 2.4 MB |
| `kde-plasma/kwin` | 6.1.5 | 8.6 MB |
| `kde-plasma/plasma-workspace` | 6.1.5 | 19.0 MB |
| `kde-plasma/plasma-meta` | listed only | none |

The GNOME, Cinnamon and CDE (Common Desktop Environment) desktops have their own chains. A package that is only listed, such as the desktop's meta package, takes no time to build.

## emerge on the system you installed

Once Gentoo starts from your own disk, emerge stays as its package manager, and it still compiles what it installs. Each of its words reaches the Mirror over the network.

Table: What emerge does on the installed system

| Words | What they do |
| --- | --- |
| `emerge <package>` | Compiles and installs the package. |
| `emerge --ask <package>` | Lists what it would build, and asks you before it starts. |
| `emerge --search [term]` | Searches the Mirror for packages whose names hold the term. |
| `emerge --sync` | Reads the Mirror's lists again. |
| `emerge --update @world` | Brings every installed package up to the version the Mirror serves. |
| `emerge --unmerge <package>` | Removes the package. |
| `emerge -pvuD @world` | Lists what emerge --update @world would merge, each package with the version you have, and merges nothing. |

```command
id: jsc:command/linux/emerge
```

## If something goes wrong

> [!PROBLEM] emerge says there are no ebuilds to satisfy the name you typed.
> Check the spelling, or use the category and name, such as kde-plasma/plasma-meta.

> [!PROBLEM] emerge says "The portage tree is empty".
> Run emerge-webrsync first ([](jsc:package_tree_and_world)).

> [!PROBLEM] emerge says "Could not resolve mirror://mainframe".
> No Mainframe running the Mirror is on this network. Connect to one, then try again.

> [!SEE] jsc:make_conf, jsc:package_tree_and_world, jsc:gentoo_reboot
