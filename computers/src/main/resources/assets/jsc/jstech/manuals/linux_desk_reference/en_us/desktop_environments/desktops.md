---
section: desktop_environments
order: 262
icon: jsc:monitor
---
# Desktops

A desktop is what a system shows when it starts with a graphical face: its windows, a panel or taskbar, and a menu that lists its programs. Without one, the system stops at its terminal, a prompt where you type commands. Each Frames edition brings its own desktop. Linux and FreeBSD do not, so you install one as a package. On UNIX, the one desktop is the Common Desktop Environment (CDE).

> [!DEFINE] Package
> A piece of software that the system's package manager installs, updates and removes, by its name.

> [!DEFINE] Desktop environment
> The graphical shell a system starts into: its windows, its panel and the menu that lists the programs it shows.

Four desktops are built in. Each has its own look, and its own names for the programs. The eras each one needs are explained at [](jsc:eras).

```table
{"caption": "The built-in desktops", "heads": ["Desktop", "Runs on", "From the era", "Disk to install", "Memory while it runs"], "rows": [[{"words": "KDE Plasma"}, {"words": "Linux, FreeBSD"}, {"words": "Legacy"}, {"amount": 256, "unit": "MB"}, {"amount": 224, "unit": "MB"}], [{"words": "GNOME"}, {"words": "Linux, FreeBSD"}, {"words": "Legacy"}, {"amount": 192, "unit": "MB"}, {"amount": 256, "unit": "MB"}], [{"words": "Cinnamon"}, {"words": "Linux, FreeBSD"}, {"words": "Standard"}, {"amount": 160, "unit": "MB"}, {"amount": 160, "unit": "MB"}], [{"words": "CDE"}, {"words": "UNIX, FreeBSD, Linux"}, {"words": "Legacy"}, {"amount": 32, "unit": "MB"}, {"amount": 16, "unit": "MB"}]]}
```

The disk and memory figures are the mod's own estimates of what each desktop takes. A Vintage computer has no graphical desktop at all: it lives at its terminal.

Each desktop wears the look of its era. On Legacy hardware KDE shows KDE 2 and GNOME shows GNOME 1. On Transition they show KDE 4 and GNOME 2. From the Standard on, KDE Plasma and GNOME have their own look. Cinnamon and CDE have one look each, on every era they run on.

A desktop shows as launchers only the programs it bundles, and only those your computer may run. KDE Plasma, GNOME and Cinnamon bundle the same ten: Network, This PC, Settings, Files, the Editor, the Command Prompt, the System Monitor, the Calculator, the Network Manager and Help. The Network Manager shows only on a Mainframe ([](jsc:mainframes)).

CDE bundles its own set. It has no This PC, so no CDE launcher shows it. Its answer to the question is Workstation Info ([](jsc:cde_workstation_info)).

A desktop you install starts at the next boot, not at the moment you install it. The computer checks what it has installed during its self-test, so restart it after an install ([](jsc:firmware)).

## If something goes wrong

> [!PROBLEM] The system starts at a shell, with no desktop.
> No desktop package is installed. Install one, as [](jsc:install_a_desktop) shows.

> [!PROBLEM] You installed a desktop, and the screen still shows the shell.
> The desktop starts at the next boot. Restart the computer.

> [!PROBLEM] The computer is a Vintage one, and the screen shows only the terminal.
> Every desktop on this page needs Legacy hardware or later, and Cinnamon needs Standard. Use a newer computer.

> [!SEE] jsc:install_a_desktop, jsc:desktop_program_names, jsc:the_systems, jsc:cde
