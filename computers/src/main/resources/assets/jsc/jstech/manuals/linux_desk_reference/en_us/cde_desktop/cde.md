---
section: cde_desktop
order: 268
icon: jsc:monitor
---
# CDE

CDE, the Common Desktop Environment, is the desktop of the Unix workstations, and the only desktop UNIX has. It runs on UNIX, FreeBSD and Linux. It is light: it needs Legacy hardware or later, 32 MB of disk and 16 MB of memory while it runs.

CDE has no login and no logout. There is nobody to log in, so you never see a login. It starts straight away, with "Starting the desktop" on its splash screen.

Where the other desktops have a start menu, a taskbar and an Action Center, CDE has one strip: the Front Panel, along the bottom centre of the screen. It has no start menu, no taskbar and no Action Center. It lists no open windows, because CDE never did: a window you put away becomes an icon on its workspace.

> [!DEFINE] Front Panel
> CDE's strip of controls at the bottom of the screen, with the workspace switch in its middle.

The Front Panel holds its controls from left to right: Clock, Calendar, File Manager, Text Editor, Style Manager, Applications, Trash Can and Help Viewer. Most of them open something, as [](jsc:cde_front_panel) lists.

A control with a small arrow at its head raises a subpanel: a tray that slides up out of the Front Panel. A subpanel stays up until you press its arrow again, and only one is up at a time.

> [!DEFINE] Subpanel
> A tray that slides up out of a Front Panel control, and stays up until its arrow is pressed again.

## Workspaces

CDE has four workspaces, named One, Two, Three and Four. Each is a separate screen of the same desktop, and each can wear its own backdrop ([](jsc:cde_style_manager)).

> [!DEFINE] Workspace
> One of four separate screens of the same desktop. A window lives on one, or on all of them.

## Windows

Each window has a title bar in the Motif style. The button at its left opens the window menu: Restore, Minimize, Maximize, Lower, Occupy Workspace..., Occupy All Workspaces and Close. A double click on that button closes the window with no menu.

> [!DEFINE] Motif
> The window style of CDE: a title bar, with a menu button at its left.

To install CDE, use the form for your system in [](jsc:install_a_desktop). On Debian and Ubuntu, that is apt install cde.

## If something goes wrong

> [!PROBLEM] You look for a Start menu, and there is none.
> CDE has no Start menu. Open Applications on the Front Panel to find your programs.

> [!PROBLEM] The package manager says "cde needs Legacy hardware or later".
> The computer is a Vintage one. Use a Legacy computer or a newer one.

> [!SEE] jsc:cde_front_panel, jsc:cde_style_manager, jsc:cde_application_manager, jsc:cde_workstation_info, jsc:cde_trash_can, jsc:install_a_desktop
