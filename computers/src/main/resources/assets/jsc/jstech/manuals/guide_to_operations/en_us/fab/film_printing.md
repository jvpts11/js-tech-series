---
section: fab
order: 43
icon: jsc:blank_film
covers: [jsc:blank_film, jsc:phototool_vintage, jsc:phototool_legacy, jsc:phototool_transition, jsc:phototool_standard, jsc:phototool_advanced]
---
# Printing a film

Two things in the chain are drawings on film: the Vintage's Layouts, the drawing of each chip its Lithography Station writes the chip's mask from, and each era's phototool, the drawing of its etched board's layers. A computer's printer prints either on Blank Film, two sheets of it made of a PVC sheet and a silver dust, its emulsion, at the Vintage's Electronics Workbench. A Layout is used up writing its mask; a phototool is never used up: a job lends it to the Board Etcher and takes it back, and one serves every board of its era.

You print the film from the computer the printer is linked to, with Midsoft Lithography, the program that writes a Lithography Station's masks.

1. Install Midsoft Lithography on the computer the printer is linked to, from its install disc.
2. Put Blank Film in the printer's tray.
3. Open the program: type litho at the prompt of MC-DOS or MC-NET, or open Lithography on the desktop.
4. Show the printer: with [[S]] on the text screen, from the Station menu in the window.
5. Pick the kind, then the film: with the arrow keys and [[Tab]] on the text screen, with the tabs and the list in the window.
6. Press [[W]] on the text screen, or Print Film in the window.

The printer prints the film in a page's time and puts it in its output, a Blank Film from its tray used for it. Any running computer prints any film on any printer.

```table
{"caption": "The program's lists", "heads": ["List", "Its films"], "rows": [[{"words": "Layouts"}, {"words": "the memory's and the controller's Layout and each processor and graphics die's of the Vintage"}], [{"words": "Phototools"}, {"words": "each era's phototool, the layers of its etched board"}]]}
```

```command
id: jsc:command/mc_net/litho
```

## If something goes wrong

> [!PROBLEM] The program says no Lithography Station is linked to this computer.
> Link a printer to this computer with its cable: the program shows a printer as it shows a station.

> [!PROBLEM] It says to put Blank Film in the printer's tray.
> The tray holds paper, a map or nothing. Put Blank Film in it, then print the film again.

> [!PROBLEM] It says the printer's queue is full.
> The printer has as many documents waiting as it holds. Print the film when one of them is out.

> [!SEE] jsc:mask_writing, jsc:board_machines
