---
section: editions
order: 232
---
# Frames 11

Frames 11 is the newest edition, the fifth. It needs an Advanced computer, the newest era with parts, and it needs the most room of the family: 20,480 MB, which is 80 items on an Advanced disk.

It installs from a USB stick in the [Dock Station](jsc:dock_station), and it keeps 8,192 MB of RAM for itself.

```table
{"caption": "Frames 11 at a glance", "rows": [[{"words": "Minimum era"}, {"words": "Advanced"}], [{"words": "Install medium"}, {"words": "USB stick, in the Dock Station"}], [{"words": "Footprint"}, {"amount": 20480, "unit": "MB"}], [{"words": "RAM held"}, {"amount": 8192, "unit": "MB"}], [{"words": "Version line"}, {"words": "Frames NT 10.0"}]]}
```

## Installing Frames 11

1. Put the Frames 11 USB stick in the Dock Station, and start the computer from it. If the computer does not start from it, open the Boot Menu ([](jsc:boot_menu)).
2. On the page "Where do you want to install Frames 11?", choose a disk.
3. On the page "Name this PC", give the computer a name.
4. Wait while the copy shows "Copying files", "Creating the system folders", "Adding the built-in programs" and "Setting up the boot entry" in turn, under the caption "Installing Frames 11".
5. Wait until the installer finishes.

The installer's pages are pale cards, with one question to a page.

Its look is Modern: a dark taskbar, centred along the bottom of the screen, and a Start menu that floats above it, with a search box and a pinned grid of programs.

The file program is called File Explorer, as on Frames 10.

Its Device Manager has the flat modern form. The visual effects are under Personalize > Visual effects, with one switch, "Animation effects". Its note reads "Windows open, close and minimize with motion, flyouts slide".

On a new computer, Frames 11 says "Hi.", as Frames 10 does. It plays "Frames 11 chimes" when it starts and "Frames 11 error" when something goes wrong. Its splash screen is near black, with the maker's mark in the middle and a ring of dots turning below.

## The Megashell

Frames 11 has its own shell, under the name Megashell. Frames 95, XP, 7 and 10 call it Command Prompt, and on Frames 11 its window and its name on the desktop are Megashell.

> [!DEFINE] Megashell
> The shell of Frames 11: the program that takes your commands, under its own name on that edition.

For the commands you type, see [The Command Prompt](jsc:command_prompt). Help on this edition opens the Get Help window ([](jsc:help_on_a_computer)).

## Edge snapping

Edge snapping tiles a window against an edge of the screen. It works on Frames 11 and on the KDE, GNOME and Cinnamon desktops of the Linux systems ([](jsc:unix_desktops)). Frames 95, XP, 7 and 10 do not snap.

> [!DEFINE] Edge snapping
> Tiling a window against an edge of the screen when you let go of it there.

> [!DEFINE] Work area
> The part of the screen that the taskbar does not cover.

Where you let go decides what the window does. A window within 4 pixels of an edge changes its size and place:

Table: Edges and what they do

| Where you let go | What the window does |
| --- | --- |
| Within 4 pixels of the top of the work area | The window maximises, and fills the work area. |
| Within 4 pixels of the left edge of the screen | The window fills the left half of the work area. |
| Within 4 pixels of the right edge of the screen | The window fills the right half of the work area. |
| Anywhere else | The window stays where you let it go. |

Snapping happens when you let go, not while you drag. Keep the window at the edge, and then release the mouse button.

A window that you snap keeps its old place. When you restore it from maximised, it returns to where it was before the snap.

## If something goes wrong

> [!PROBLEM] A window let go in the middle of the screen does not tile.
> Only the edges in the table snap a window. Drag the window to one of them, and let go.

> [!PROBLEM] A window does not tile on Frames 10.
> Frames 10 has no edge snapping. Move and resize the window by hand.

## The GPU page

The GPU page is in Task Manager, and it shows the graphics processing unit (GPU) of the computer: the card that draws the picture for the monitors. Frames 10 has the same page. It shows how busy the card is, and which monitors and windows use its video memory.

> [!DEFINE] Video memory
> The memory on a graphics card, which the monitors and the graphics windows use.

Table: What the GPU page shows

| Part of the page | What it tells you |
| --- | --- |
| Title and card name | GPU, and the card's name on the right in small text. |
| 3D graph | How busy the card is, from 0 to 100. |
| Memory graph | How full the memory is: "Dedicated GPU memory usage" for a card, or "Shared GPU memory usage" for integrated graphics. |
| Utilization | How busy the card is, as a percent. |
| Dedicated memory, or Shared memory | The memory in use, out of the total. |
| Cores | The number of cores, and the clock speed in MHz or GHz. |
| Slot | The PCI Express (PCIe) slot the card sits in, or "On the processor" for integrated graphics. |
| Used by | One row for each monitor or graphics window that holds video memory, with its size. |

The page shows the first graphics card in the computer's build. With no card, it shows the graphics of the processor, if the processor has any, and the memory is then shared with the computer.

The utilisation percentage grows with two things. The memory fill adds up to 50 percent, and each open graphics window adds 20 percent. It is 0 when no video memory is in use, and it never goes above 100 percent.

A lit monitor holds video memory in blocks, and a graphics window takes a quarter of a monitor block, or a whole block while it fills the screen. A block's size depends on the era of the computer.

```table
{"caption": "Video memory by era", "heads": ["Era", "One monitor block", "Window", "Full-screen window"], "rows": [[{"words": "Vintage"}, {"amount": 256, "unit": "KB"}, {"amount": 64, "unit": "KB"}, {"amount": 256, "unit": "KB"}], [{"words": "Legacy"}, {"amount": 4, "unit": "MB"}, {"amount": 1, "unit": "MB"}, {"amount": 4, "unit": "MB"}], [{"words": "Transition"}, {"amount": 16, "unit": "MB"}, {"amount": 4, "unit": "MB"}, {"amount": 16, "unit": "MB"}], [{"words": "Standard"}, {"amount": 64, "unit": "MB"}, {"amount": 16, "unit": "MB"}, {"amount": 64, "unit": "MB"}], [{"words": "Advanced"}, {"amount": 256, "unit": "MB"}, {"amount": 64, "unit": "MB"}, {"amount": 256, "unit": "MB"}]]}
```

These sizes are estimates. The table gives the 16-colour block; a Vintage monitor that shows only black and white uses 64 KB per block. The page writes an amount in KB below 1 MB, in MB below 1 GB, and in GB above that, with one decimal place.

## If something goes wrong

> [!PROBLEM] The monitor says "The computer has no video output: install a graphics card".
> Install a graphics card in the computer ([](jsc:graphics_cards)).

> [!PROBLEM] The monitor stays dark and says "Not enough video memory". It lights when the card has room.
> Turn off a monitor, or use a card with more memory ([](jsc:graphics_cards)).

> [!PROBLEM] A graphics window does not open, and the desktop says "Not enough video memory".
> Turn off a monitor or close a graphics program.

> [!PROBLEM] The GPU page says "This computer has no graphics."
> The computer has no graphics card and no graphics on its processor. Install a graphics card ([](jsc:graphics_cards)).

> [!SEE] jsc:graphics_cards, jsc:monitors, jsc:frames_10
