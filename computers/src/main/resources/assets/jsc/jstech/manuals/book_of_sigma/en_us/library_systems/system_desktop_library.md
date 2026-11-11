---
section: library_systems
order: 478
---
# IDesktop and Desktop: a desktop of your own

A desktop of your own is a class that implements IDesktop. It declares its look, and brings its own programs in Sigma#. The mod moves its windows and keeps the focus, so it is as smooth as the mod's desktops.

Table: What a desktop declares

| Property | What it says |
| --- | --- |
| `Name, Version, Author` | The desktop's package name, its version and who wrote it. |
| `Menu, PanelAt` | The word on the panel that opens the menu, and where the panel runs: top or bottom. |
| `Frames` | The style of the window frames: flat, bevel, luna, aero, metro, kde2, gnome1, oxygen, clearlooks, motif or platinum. |
| `DesktopColour, PanelColour, TextColour, AccentColour, WindowColour, TitleColour` | Its colours, each written #RRGGBB. |
| `Icons, Wallpaper` | The folder of its programs' pictures, and the picture it hangs. |
| `Programs` | Its programs, named in one text with commas. |

Table: Desktop members

| Member | What it does |
| --- | --- |
| `Desktop.Start(package)` | Starts a desktop: one of the mod's, or one a player built. |
| `Desktop.Stop()` | Goes back to the system's text screen or prompt. |
| `Desktop.Current` | The desktop that is up, or nothing. |

sigma build --desktop makes the desktop's package. A system that starts it brings it, and the package managers of the mod's systems install it from the Mirror.

## If something goes wrong

> [!PROBLEM] Desktop.Start stops the system and says there is no desktop of that name.
> Build the desktop with sigma build --desktop, and start it by its package name.

> [!PROBLEM] Desktop.Start says the system drives no graphics screen.
> Add graphics to the kernel's drivers.

> [!SEE] jsc:system_os_library
