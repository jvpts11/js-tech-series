---
section: desktop_environments
order: 264
icon: jsc:monitor
---
# KDE Plasma

KDE Plasma is the desktop of the KDE project. It runs on Linux and FreeBSD, on Legacy hardware or later, and its package is named kde-plasma. Its house is the KDE Guild, a software house of the mod's world.

KDE changes its face with the era. On Legacy hardware you see KDE 2. On Transition you see KDE 4, with its Kickoff 4 menu. From the Standard on you see KDE Plasma.

> [!DEFINE] Kickoff
> KDE's start menu: a launcher you open from the panel, with a list of programs beside its places.

The panel sits along the bottom of the screen. On KDE Plasma, Kickoff is a two-pane launcher: a column of places beside the list of programs. Type in it, and the search looks through every program. KDE 4 shows the tabs Favorites, Applications, Computer, Recently Used and Leave.

KDE gives seven programs their own names, as the table shows. The rest keep the names they have on every desktop ([](jsc:desktop_program_names)).

Table: KDE's own names

| Program | Name on KDE Plasma |
| --- | --- |
| Files | Dolphin |
| Editor | Kate |
| Command Prompt | Konsole |
| Calculator | KCalc |
| This PC | Info Center |
| Settings | System Settings |
| Help | Help Center |

To install it, use the form for your system in [](jsc:install_a_desktop). On Debian and Ubuntu, that is apt install kde-plasma. The boot lines call it kde plasma, as in "Starting kde plasma Display Manager".

> [!DEFINE] Display manager
> The boot step that starts a graphical desktop. The boot lines name the desktop it starts.

## If something goes wrong

> [!PROBLEM] The package manager says "kde-plasma needs Legacy hardware or later".
> The computer is a Vintage one. Use a Legacy computer or a newer one.

> [!PROBLEM] The package manager says "unmet requirements (hardware or free disk space)".
> Plasma takes 256 MB of disk. Free some disk space, then try again.

> [!SEE] jsc:install_a_desktop, jsc:desktop_program_names, jsc:eras
